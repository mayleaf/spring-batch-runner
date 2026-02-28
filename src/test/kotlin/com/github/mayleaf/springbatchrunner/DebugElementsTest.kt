package com.github.mayleaf.springbatchrunner

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.psi.KtClass

/**
 * Debug test to understand PSI element structure.
 */
class DebugElementsTest : BasePlatformTestCase() {

    fun testKotlinElementStructure() {
        myFixture.configureByText(
            "TestClass.kt",
            """
            import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
            import org.springframework.context.annotation.Configuration

            @Configuration
            @ConditionalOnProperty(name = ["spring.batch.job.names"], havingValue = "testJob")
            class TestBatchJob
            """.trimIndent()
        )

        val ktClass = PsiTreeUtil.findChildOfType(myFixture.file, KtClass::class.java)
        assertNotNull("Kotlin class should be found", ktClass)

        val identifier = ktClass!!.nameIdentifier
        assertNotNull("Class identifier should be found", identifier)

        println("Identifier class: ${identifier!!.javaClass.name}")
        println("Identifier text: ${identifier.text}")
        println("Identifier parent class: ${identifier.parent?.javaClass?.name}")
        println("Parent is KtClass: ${identifier.parent is KtClass}")

        val lightClass = ktClass.toLightClass()
        println("Light class: $lightClass")
        println("Light class modifiers: ${lightClass?.modifierList}")

        val annotations = lightClass?.modifierList?.annotations
        println("Light class annotations count: ${annotations?.size}")
        annotations?.forEach { annotation ->
            println("Annotation: ${annotation.javaClass.name}")
            println("Annotation qualified name: ${annotation.qualifiedName}")
            println("Annotation text: ${annotation.text}")
        }
    }
}
