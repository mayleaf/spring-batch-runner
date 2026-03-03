package com.github.mayleaf.springbatchrunner

import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.application.ApplicationConfigurationType
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.impl.RunDialog
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.util.Processor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiField
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiPolyadicExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtSimpleNameStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateEntryWithExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtUserType

object RunSpringBatchJobAction {
    private const val SPRING_BOOT_APPLICATION_FQN =
        "org.springframework.boot.autoconfigure.SpringBootApplication"

    private const val VALUE_ANNOTATION_FQN =
        "org.springframework.beans.factory.annotation.Value"

    private const val BEAN_ANNOTATION_FQN =
        "org.springframework.context.annotation.Bean"

    private val COMPONENT_ANNOTATION_FQNS = listOf(
        "org.springframework.stereotype.Component",
        "org.springframework.batch.core.configuration.annotation.StepScope",
        "org.springframework.batch.core.configuration.annotation.JobScope",
    )

    private val JOB_PARAM_PATTERN = Regex("""#\{jobParameters\[['"]?(\w+)['"]?]""")

    fun run(project: Project, context: PsiElement, jobName: String) {
        val runManager = RunManager.getInstance(project)
        val configName = "Spring Batch: $jobName"


        val settings: RunnerAndConfigurationSettings = runManager.findConfigurationByName(configName)
            ?: createConfiguration(runManager, project, configName, context, jobName)


        runManager.selectedConfiguration = settings
        val executor = DefaultRunExecutor.getRunExecutorInstance()
        if(RunDialog.editConfiguration(project, settings, "Edit Configuration: $configName", executor)) {
            runManager.addConfiguration(settings)
            runManager.selectedConfiguration = settings
            ProgramRunnerUtil.executeConfiguration(settings, executor)
        }
    }

    private fun createConfiguration(runManager: RunManager, project: Project, configName: String, context: PsiElement, jobName: String) : RunnerAndConfigurationSettings {
        val configurationType = ApplicationConfigurationType.getInstance()
        val factory = configurationType.configurationFactories.first()
        val settings = runManager.createConfiguration(configName, factory)
        val configuration = settings.configuration as ApplicationConfiguration

        val module = ModuleUtilCore.findModuleForPsiElement(context)


        val mainClassName = findMainClassInModule(project, module)
        if(mainClassName != null) {
            configuration.mainClassName = mainClassName
        }

        val jobParams = extractJobParameters(context)
        val paramsBuilder = StringBuilder("--spring.batch.job.names=$jobName")
        jobParams.forEach { paramsBuilder.append(" $it=") }
        configuration.programParameters = paramsBuilder.toString()

        if (module != null) {
            configuration.setModule(module)
        }
        settings.isTemporary = true
        return settings
    }

    private fun extractJobParameters(context: PsiElement): List<String> {
        val project = context.project
        val module = ModuleUtilCore.findModuleForPsiElement(context)
        return when (context) {
            is PsiClass -> extractJavaJobParameters(context, project, module)
            is KtClassOrObject -> extractKotlinJobParameters(context, project, module)
            else -> emptyList()
        }
    }

    private fun extractJavaJobParameters(psiClass: PsiClass, project: Project, module: Module?): List<String> {
        val params = mutableSetOf<String>()
        scanJavaClassForValues(psiClass, params)
        for (method in psiClass.methods) {
            if (method.getAnnotation(BEAN_ANNOTATION_FQN) == null) continue
            for (param in method.parameterList.parameters) {
                val refClass = (param.type as? PsiClassType)?.resolve() ?: continue
                if (refClass != psiClass) scanJavaClassForValues(refClass, params)
            }
            val returnClass = (method.returnType as? PsiClassType)?.resolve()
            if (returnClass != null && returnClass != psiClass) scanJavaClassForValues(returnClass, params)
        }
        scanComponentClasses(project, module, params)
        return params.toList()
    }

    private fun scanJavaClassForValues(psiClass: PsiClass, params: MutableSet<String>) {
        for (field in psiClass.fields) {
            extractFromJavaAnnotation(field.getAnnotation(VALUE_ANNOTATION_FQN), params)
        }
        for (method in psiClass.methods) {
            for (param in method.parameterList.parameters) {
                extractFromJavaAnnotation(param.getAnnotation(VALUE_ANNOTATION_FQN), params)
            }
        }
    }

    private fun extractFromJavaAnnotation(annotation: PsiAnnotation?, params: MutableSet<String>) {
        annotation ?: return
        val value = annotation.findAttributeValue("value") ?: return
        val text = resolveJavaExpression(value as? PsiExpression) ?: return
        JOB_PARAM_PATTERN.findAll(text).forEach { params.add(it.groupValues[1]) }
    }

    /**
     * Resolve Java expression to string, handling:
     * - Simple string literals: "value"
     * - String concatenation: "prefix" + CONSTANT + "suffix"
     * - Static field references: JobConfig.PARAM_NAME
     */
    private fun resolveJavaExpression(expr: PsiExpression?): String? {
        expr ?: return null
        return when (expr) {
            is PsiLiteralExpression -> expr.value as? String
            is PsiPolyadicExpression -> {
                // Handle string concatenation: "#{jobParameters[" + CONSTANT + "]}"
                val parts = expr.operands.mapNotNull { resolveJavaExpression(it) }
                if (parts.size == expr.operands.size) parts.joinToString("") else null
            }
            is PsiReferenceExpression -> {
                // Handle static field reference: JobConfig.PARAM_NAME or just PARAM_NAME
                val resolved = expr.resolve()
                if (resolved is PsiField && resolved.hasModifierProperty(PsiModifier.STATIC)) {
                    resolveJavaExpression(resolved.initializer)
                } else null
            }
            else -> expr.text?.removeSurrounding("\"")
        }
    }

    private fun extractKotlinJobParameters(ktClass: KtClassOrObject, project: Project, module: Module?): List<String> {
        val params = mutableSetOf<String>()
        scanKotlinClassForValues(ktClass, params)
        for (func in ktClass.declarations.filterIsInstance<KtNamedFunction>()) {
            if (func.annotationEntries.none { it.shortName?.asString() == "Bean" }) continue
            for (param in func.valueParameters) {
                scanKtResolvedElement(resolveKtParamType(param), ktClass, params)
            }
            scanKtResolvedElement(resolveKtFunctionReturnType(func), ktClass, params)
        }
        scanComponentClasses(project, module, params)
        return params.toList()
    }

    private fun scanKtResolvedElement(resolved: PsiElement?, ownerClass: KtClassOrObject, params: MutableSet<String>) {
        resolved ?: return
        val ktRef = resolved as? KtClassOrObject
            ?: (resolved as? PsiClass)?.navigationElement as? KtClassOrObject
        val javaRef = if (ktRef == null) resolved as? PsiClass else null
        when {
            ktRef != null && ktRef != ownerClass -> scanKotlinClassForValues(ktRef, params)
            javaRef != null -> scanJavaClassForValues(javaRef, params)
        }
    }

    private fun scanKotlinClassForValues(ktClass: KtClassOrObject, params: MutableSet<String>) {
        for (prop in ktClass.declarations.filterIsInstance<KtProperty>()) {
            extractFromKtAnnotations(prop.annotationEntries, params)
        }
        ktClass.primaryConstructor?.valueParameters?.forEach { param ->
            extractFromKtAnnotations(param.annotationEntries, params)
        }
        for (func in ktClass.declarations.filterIsInstance<KtNamedFunction>()) {
            func.valueParameters.forEach { param ->
                extractFromKtAnnotations(param.annotationEntries, params)
            }
        }
    }

    private fun resolveKtParamType(param: KtParameter): PsiElement? {
        val typeElement = param.typeReference?.typeElement as? KtUserType ?: return null
        return typeElement.referenceExpression?.references?.firstOrNull()?.resolve()
    }

    private fun resolveKtFunctionReturnType(func: KtNamedFunction): PsiElement? {
        val typeElement = func.typeReference?.typeElement as? KtUserType ?: return null
        return typeElement.referenceExpression?.references?.firstOrNull()?.resolve()
    }

    private fun extractFromKtAnnotations(annotations: List<KtAnnotationEntry>, params: MutableSet<String>) {
        for (annotation in annotations) {
            if (annotation.shortName?.asString() != "Value") continue
            val arg = annotation.valueArgumentList?.arguments?.firstOrNull() ?: continue
            val expr = arg.getArgumentExpression() as? KtStringTemplateExpression ?: continue
            val text = resolveKtStringTemplate(expr) ?: continue
            JOB_PARAM_PATTERN.findAll(text).forEach { params.add(it.groupValues[1]) }
        }
    }

    /**
     * Resolve Kotlin string template to string, handling:
     * - Simple strings: "value"
     * - String templates with variables: "#{jobParameters[$CONSTANT]}"
     * - Companion object properties: "#{jobParameters[${JobConfig.PARAM_NAME}]}"
     */
    private fun resolveKtStringTemplate(expr: KtStringTemplateExpression): String? {
        val parts = expr.entries.map { entry ->
            when (entry) {
                is KtSimpleNameStringTemplateEntry -> {
                    // Handle $variable
                    resolveKtReference(entry.expression)
                }
                is KtStringTemplateEntryWithExpression -> {
                    // Handle ${expression}
                    resolveKtReference(entry.expression)
                }
                else -> entry.text
            }
        }
        return if (parts.all { it != null }) parts.joinToString("") else null
    }

    /**
     * Resolve Kotlin reference expression to its constant value
     */
    private fun resolveKtReference(expr: PsiElement?): String? {
        expr ?: return null
        return when (expr) {
            is KtDotQualifiedExpression -> {
                // Handle Companion.CONSTANT or ClassName.CONSTANT
                val resolved = expr.selectorExpression?.references?.firstOrNull()?.resolve()
                resolveKtPropertyValue(resolved)
            }
            is KtNameReferenceExpression -> {
                // Handle simple CONSTANT reference
                val resolved = expr.references.firstOrNull()?.resolve()
                resolveKtPropertyValue(resolved)
            }
            else -> expr.text
        }
    }

    /**
     * Resolve Kotlin property or Java field to its constant value
     */
    private fun resolveKtPropertyValue(resolved: PsiElement?): String? {
        return when (resolved) {
            is KtProperty -> {
                // Kotlin property: const val PARAM = "value"
                val initializer = resolved.initializer
                when (initializer) {
                    is KtStringTemplateExpression -> resolveKtStringTemplate(initializer)
                    else -> initializer?.text?.removeSurrounding("\"")
                }
            }
            is PsiField -> {
                // Java static field
                if (resolved.hasModifierProperty(PsiModifier.STATIC)) {
                    resolveJavaExpression(resolved.initializer)
                } else null
            }
            else -> null
        }
    }

    private fun scanComponentClasses(project: Project, module: Module?, params: MutableSet<String>) {
        val scope = if (module != null)
            GlobalSearchScope.moduleScope(module)
        else
            GlobalSearchScope.projectScope(project)
        val allScope = GlobalSearchScope.allScope(project)
        val psiFacade = JavaPsiFacade.getInstance(project)
        for (fqn in COMPONENT_ANNOTATION_FQNS) {
            val annotationClass = psiFacade.findClass(fqn, allScope) ?: continue
            AnnotatedElementsSearch.searchPsiClasses(annotationClass, scope).forEach(Processor { cls ->
                val ktClass = cls.navigationElement as? KtClassOrObject
                if (ktClass != null) {
                    scanKotlinClassForValues(ktClass, params)
                } else {
                    scanJavaClassForValues(cls, params)
                }
                true
            })
        }
    }

    private fun findMainClassInModule(project: Project, module: Module?): String? {
        val annotationClass = JavaPsiFacade.getInstance(project).findClass(SPRING_BOOT_APPLICATION_FQN,
            GlobalSearchScope.allScope(project)) ?: return null

        if(module != null) {
            val moduleScope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module)
            val found = searchForSpringBootMain(annotationClass, moduleScope)
            if (found != null) return found
        }

        val projectScope = GlobalSearchScope.projectScope(project)
        return searchForSpringBootMain(annotationClass, projectScope)
    }

    private fun searchForSpringBootMain(annotationClass: PsiClass, scope: GlobalSearchScope): String? {
        var result: String? = null
        AnnotatedElementsSearch.searchPsiClasses(annotationClass, scope)
            .forEach(Processor { cls ->
                if(hasMainMethod(cls)) {
                    result = cls.qualifiedName
                    return@Processor false
                }
                val ktFacadeName = findKotlinFacadeName(cls)
                if (ktFacadeName != null) {
                    result = ktFacadeName
                    return@Processor false
                }
                true
            })
        return result
    }

    private fun findKotlinFacadeName(psiClass: PsiClass): String? {
        val ktClass = psiClass.navigationElement as? KtClassOrObject ?: return null
        val ktFile = ktClass.containingFile as? KtFile ?: return null
        val hasTopLevelMain = ktFile.declarations.filterIsInstance<KtNamedFunction>()
            .any { it.name == "main" }
        if (!hasTopLevelMain) return null

        val packageFqName = ktFile.packageFqName.asString()
        val fileNameWithoutExt = ktFile.name.removeSuffix(".kt")
        return if (packageFqName.isEmpty()) "${fileNameWithoutExt}Kt"
               else "$packageFqName.${fileNameWithoutExt}Kt"
    }

    private fun hasMainMethod(psiClass: PsiClass): Boolean {
        return psiClass.methods.any { method ->
            method.name == "main" &&
                    method.hasModifierProperty(PsiModifier.STATIC) &&
                    method.hasModifierProperty(PsiModifier.PUBLIC)
        }
    }


}