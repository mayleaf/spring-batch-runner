package com.github.mayleaf.springbatchrunner

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.JavaTokenType
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiBinaryExpression
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiParenthesizedExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiVariable
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtCollectionLiteralExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

/**
 * Provides gutter icons (line markers) for running Spring Batch jobs directly from the IDE.
 *
 * This provider detects classes annotated with @ConditionalOnProperty where:
 * - The property name is "spring.batch.job.names"
 * - The havingValue attribute contains the batch job name
 *
 * When detected, it displays a run icon in the gutter that allows developers to execute
 * the Spring Batch job with a single click.
 *
 * Example usage in code:
 * ```kotlin
 * @Configuration
 * @ConditionalOnProperty(name = "spring.batch.job.names", havingValue = "myJobName")
 * class MyBatchJobConfiguration {
 *     // Batch job configuration...
 * }
 * ```
 */
class SpringBatchRunLineMarkerProvider : LineMarkerProvider {

    companion object {
        /**
         * Fully qualified name of Spring Boot's ConditionalOnProperty annotation.
         */
        private const val CONDITIONAL_ON_PROPERTY_FQN =
            "org.springframework.boot.autoconfigure.condition.ConditionalOnProperty"

        /**
         * The property names used to configure Spring Batch job names.
         * Supports both plural and singular forms.
         */
        private val TARGET_PROPERTY_NAMES = setOf("spring.batch.job.names", "spring.batch.job.name")
    }

    /**
     * Creates a line marker for classes that represent Spring Batch jobs.
     *
     * This method is called for every PSI element in the file. It checks if the element
     * is a class identifier and whether the class is configured as a Spring Batch job
     * (via @ConditionalOnProperty annotation).
     *
     * Supports both Java and Kotlin classes.
     *
     * @param element The PSI element to inspect (typically a class identifier)
     * @return A LineMarkerInfo with a run icon if this is a batch job class, null otherwise
     */
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        // Handle Java classes
        if (element is PsiIdentifier) {
            val psiClass = element.parent as? PsiClass ?: return null
            val jobName = extractBatchJobName(psiClass) ?: return null

            return LineMarkerInfo(
                element,
                element.textRange,
                AllIcons.RunConfigurations.TestState.Run,
                { "Run Spring Batch Job '$jobName'" },
                { _, _ ->
                    RunSpringBatchJobAction.run(psiClass.project, psiClass, jobName)
                },
                GutterIconRenderer.Alignment.CENTER,
                { "Run Spring Batch Job '$jobName'" }
            )
        }

        // Handle Kotlin classes - check if this element is a class name identifier
        val ktClass = element.parent as? KtClass ?: return null
        if (element != ktClass.nameIdentifier) return null

        val jobName = extractKtBatchJobName(ktClass) ?: return null

        return LineMarkerInfo(
            element,
            element.textRange,
            AllIcons.RunConfigurations.TestState.Run,
            { "Run Spring Batch Job '$jobName'" },
            { _, _ ->
                RunSpringBatchJobAction.run(ktClass.project, ktClass, jobName)
            },
            GutterIconRenderer.Alignment.CENTER,
            { "Run Spring Batch Job '$jobName'" }
        )
    }

    /**
     * Extracts the batch job name from a class annotated with @ConditionalOnProperty.
     *
     * This method searches for the annotation on the class itself, or on its containing class
     * (for inner classes). It looks for annotations where:
     * - name/value attribute contains "spring.batch.job.names"
     * - havingValue attribute contains the job name
     *
     * @param psiClass The class to inspect
     * @return The batch job name if found, null otherwise
     */
    private fun extractBatchJobName(psiClass: PsiClass): String? {
        return findConditionalOnPropertyValue(psiClass.modifierList)
            ?: findConditionalOnPropertyValue(psiClass.containingClass?.modifierList)
    }

    /**
     * Searches for @ConditionalOnProperty annotation and extracts the job name.
     *
     * This method iterates through all annotations in the modifier list and checks if any
     * @ConditionalOnProperty annotation has the correct "name" attribute and non-blank
     * "havingValue" attribute.
     *
     * @param modifierList The modifier list containing annotations
     * @return The job name from the havingValue attribute, or null if not found
     */
    private fun findConditionalOnPropertyValue(modifierList: PsiModifierList?): String? {
        if (modifierList == null) return null
        for (annotation in modifierList.annotations) {
            val qualifiedName = annotation.qualifiedName ?: continue
            if (qualifiedName != CONDITIONAL_ON_PROPERTY_FQN) continue

            if (!hasMatchingNameAttribute(annotation)) continue

            val havingValue = annotation.findAttributeValue("havingValue")
            val jobName = resolveStringValue(havingValue)
            if (!jobName.isNullOrBlank()) return jobName
        }
        return null
    }

    /**
     * Checks if the annotation's "name" or "value" attribute contains a target property name.
     *
     * This method handles multiple forms of the attribute value:
     * - Literal strings: "spring.batch.job.names" or "spring.batch.job.name"
     * - Array literals: ["spring.batch.job.names"]
     * - Reference expressions: pointing to constants
     * - Binary expressions: string concatenation
     *
     * @param annotation The annotation to check
     * @return true if the name/value attribute matches any TARGET_PROPERTY_NAMES, false otherwise
     */
    private fun hasMatchingNameAttribute(annotation: PsiAnnotation): Boolean {
        val nameAttr = annotation.findAttributeValue("name")
            ?: annotation.findAttributeValue("value")
            ?: return false

        return when (nameAttr) {
            is PsiLiteralExpression -> nameAttr.value in TARGET_PROPERTY_NAMES
            is PsiArrayInitializerMemberValue -> {
                nameAttr.initializers.any { resolveStringValue(it) in TARGET_PROPERTY_NAMES }
            }

            is PsiReferenceExpression -> resolveReferenceToString(nameAttr) in TARGET_PROPERTY_NAMES
            else -> {
                val resolved = resolveStringValue(nameAttr)
                if (resolved != null) return resolved in TARGET_PROPERTY_NAMES

                val text = nameAttr.text?.removeSurrounding("\"")
                    ?.removeSurrounding("[", "]")
                    ?.split(",")
                    ?.map { it.trim().removeSurrounding("\"") }
                text?.any { it in TARGET_PROPERTY_NAMES } == true
            }
        }
    }

    /**
     * Resolves annotation attribute values to their string representation.
     *
     * This method handles various forms of attribute values:
     * - Literal expressions: "jobName"
     * - Reference expressions: pointing to constants or variables
     * - Binary expressions: string concatenation (e.g., PREFIX + "jobName")
     * - Parenthesized expressions: (expression)
     * - Plain text: as a fallback
     *
     * @param expression The annotation member value to resolve
     * @return The resolved string value, or null if it cannot be resolved
     */
    private fun resolveStringValue(expression: PsiAnnotationMemberValue?): String? {
        if (expression == null) return null
        return when (expression) {
            is PsiLiteralExpression -> expression.value as? String

            is PsiReferenceExpression -> resolveReferenceToString(expression)
            is PsiBinaryExpression -> resolveBinaryExpression(expression)
            is PsiParenthesizedExpression -> resolveStringValue(expression.expression as? PsiAnnotationMemberValue)
            else -> {
                val text = expression.text?.trim()?.removeSurrounding("\"")
                if (text.isNullOrBlank()) null else text
            }
        }
    }

    /**
     * Resolves a reference expression (e.g., constant or variable) to its string value.
     *
     * This method handles:
     * - Field references: static/instance fields
     * - Variable references: local variables
     *
     * @param ref The reference expression to resolve
     * @return The resolved string value, or null if it cannot be resolved
     */
    private fun resolveReferenceToString(ref: PsiReferenceExpression): String? {
        val resolved = ref.resolve()

        if (resolved is PsiField) {
            return resolveFieldValue(resolved)
        }

        if (resolved is PsiVariable) {
            val initializer = resolved.initializer
            if (initializer is PsiLiteralExpression) return initializer.value as? String
            if (initializer is PsiAnnotationMemberValue) return resolveStringValue(initializer)
        }

        return null
    }

    /**
     * Resolves a field's value to its string representation.
     *
     * This method first attempts to compute the constant value (for compile-time constants),
     * then falls back to resolving the field's initializer expression.
     *
     * @param field The field to resolve
     * @return The resolved string value, or null if it cannot be resolved
     */
    private fun resolveFieldValue(field: PsiField): String? {
        val constantValue = field.computeConstantValue()
        if (constantValue is String) return constantValue

        val initializer = field.initializer
        if (initializer is PsiAnnotationMemberValue) {
            return resolveStringValue(initializer)
        }

        if (initializer is PsiLiteralExpression) {
            return initializer.value as? String
        }

        return null
    }

    /**
     * Resolves binary expressions (typically string concatenation) to their combined value.
     *
     * This method only handles the '+' operator for string concatenation.
     * Both operands must be resolvable to strings.
     *
     * @param expr The binary expression to resolve
     * @return The concatenated string value, or null if it cannot be resolved
     */
    private fun resolveBinaryExpression(expr: PsiBinaryExpression): String? {
        if (expr.operationTokenType != JavaTokenType.PLUS) return null

        val left = resolveStringValue(expr.lOperand as? PsiAnnotationMemberValue)
        val right = resolveStringValue(expr.rOperand as? PsiAnnotationMemberValue)

        if (left != null && right != null) return left + right

        return null
    }

    // ---- Kotlin PSI support (no toLightClass) ----

    private fun extractKtBatchJobName(ktClass: KtClass): String? {
        return findKtConditionalOnPropertyValue(ktClass.annotationEntries)
            ?: (ktClass.parent as? KtClassOrObject)?.let {
                findKtConditionalOnPropertyValue(it.annotationEntries)
            }
    }

    private fun findKtConditionalOnPropertyValue(annotations: List<KtAnnotationEntry>): String? {
        for (annotation in annotations) {
            val fqName = annotation.shortName?.asString()
            if (fqName != "ConditionalOnProperty") continue

            if (!hasMatchingKtNameAttribute(annotation)) continue

            val havingValue = getKtArgumentValue(annotation, "havingValue")
            if (!havingValue.isNullOrBlank()) return havingValue
        }
        return null
    }

    private fun hasMatchingKtNameAttribute(annotation: KtAnnotationEntry): Boolean {
        val nameValue = getKtArgumentExpression(annotation, "name")
            ?: getKtArgumentExpression(annotation, "value")
            ?: return false

        return when (nameValue) {
            is KtStringTemplateExpression -> resolveKtStringValue(nameValue) in TARGET_PROPERTY_NAMES
            is KtCollectionLiteralExpression -> {
                nameValue.innerExpressions.any { resolveKtStringValue(it) in TARGET_PROPERTY_NAMES }
            }
            else -> resolveKtStringValue(nameValue) in TARGET_PROPERTY_NAMES
        }
    }

    private fun getKtArgumentExpression(annotation: KtAnnotationEntry, name: String): org.jetbrains.kotlin.psi.KtExpression? {
        val args = annotation.valueArgumentList?.arguments ?: return null
        return args.firstOrNull { it.getArgumentName()?.asName?.asString() == name }
            ?.getArgumentExpression()
    }

    private fun getKtArgumentValue(annotation: KtAnnotationEntry, name: String): String? {
        val expr = getKtArgumentExpression(annotation, name) ?: return null
        return resolveKtStringValue(expr)
    }

    private fun resolveKtStringValue(expr: org.jetbrains.kotlin.psi.KtExpression?): String? {
        if (expr == null) return null
        return when (expr) {
            is KtStringTemplateExpression -> {
                if (expr.hasInterpolation()) return null
                expr.entries.joinToString("") { it.text }
            }
            is KtDotQualifiedExpression -> resolveKtConstantReference(expr)
            is KtNameReferenceExpression -> resolveKtSimpleReference(expr)
            else -> null
        }
    }

    private fun resolveKtConstantReference(expr: KtDotQualifiedExpression): String? {
        val propertyName = (expr.selectorExpression as? KtNameReferenceExpression)?.getReferencedName() ?: return null

        // Resolve the reference through PSI first — handles all forms including
        // multi-level qualified names like Configuration.Companion.JOB_NAME
        val ref = expr.selectorExpression?.references?.firstOrNull()
        val resolved = ref?.resolve()
        if (resolved is KtProperty) {
            val initializer = resolved.initializer
            if (initializer is KtStringTemplateExpression) {
                return resolveKtStringValue(initializer)
            }
        }

        // Fallback: search siblings for a top-level object/companion with matching property
        // Only applicable for single-level references like Companion.JOB_NAME
        val receiverName = (expr.receiverExpression as? KtNameReferenceExpression)?.getReferencedName() ?: return null
        val file = expr.containingFile ?: return null
        for (child in file.children) {
            if (child is KtObjectDeclaration && child.name == receiverName) {
                val prop = child.declarations.filterIsInstance<KtProperty>()
                    .firstOrNull { it.name == propertyName }
                val initializer = prop?.initializer
                if (initializer is KtStringTemplateExpression) {
                    return resolveKtStringValue(initializer)
                }
            }
        }
        return null
    }

    private fun resolveKtSimpleReference(expr: KtNameReferenceExpression): String? {
        val ref = expr.references.firstOrNull()
        val resolved = ref?.resolve()
        if (resolved is KtProperty) {
            val initializer = resolved.initializer
            if (initializer is KtStringTemplateExpression) {
                return resolveKtStringValue(initializer)
            }
        }
        return null
    }
}