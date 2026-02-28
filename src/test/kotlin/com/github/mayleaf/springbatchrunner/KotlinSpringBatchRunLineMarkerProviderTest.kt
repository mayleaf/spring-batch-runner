package com.github.mayleaf.springbatchrunner

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.kotlin.psi.KtClass

/**
 * Test cases for SpringBatchRunLineMarkerProvider with Kotlin classes.
 */
class KotlinSpringBatchRunLineMarkerProviderTest : BasePlatformTestCase() {

    private lateinit var provider: SpringBatchRunLineMarkerProvider

    override fun setUp() {
        super.setUp()
        provider = SpringBatchRunLineMarkerProvider()
    }

    /**
     * Tests that a line marker is created for a Kotlin class with @ConditionalOnProperty.
     */
    fun testLineMarkerForKotlinClass() {
        myFixture.configureByText(
            "TestBatchJob.kt",
            """
            import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
            import org.springframework.context.annotation.Configuration

            @Configuration
            @ConditionalOnProperty(name = ["spring.batch.job.names"], havingValue = "kotlinJob")
            class TestBatchJob
            """.trimIndent()
        )

        val ktClass = PsiTreeUtil.findChildOfType(myFixture.file, KtClass::class.java)
        assertNotNull("Kotlin class should be found", ktClass)

        val nameIdentifier = ktClass!!.nameIdentifier
        assertNotNull("Class name identifier should be found", nameIdentifier)

        println("Element type: ${nameIdentifier!!.javaClass.name}")
        println("Element text: ${nameIdentifier.text}")
        println("Element parent: ${nameIdentifier.parent?.javaClass?.name}")

        val lineMarkerInfo = provider.getLineMarkerInfo(nameIdentifier)
        assertNotNull("Line marker should be created for Kotlin batch job class", lineMarkerInfo)
        assertTrue("Tooltip should contain job name", lineMarkerInfo!!.lineMarkerTooltip!!.contains("kotlinJob"))
    }

    /**
     * Tests that a line marker is created for a Kotlin class with singular property name.
     */
    fun testLineMarkerForKotlinClassWithSingular() {
        myFixture.configureByText(
            "SingleJob.kt",
            """
            import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
            import org.springframework.context.annotation.Configuration

            @Configuration
            @ConditionalOnProperty(name = ["spring.batch.job.name"], havingValue = "singleKotlinJob")
            class SingleJob
            """.trimIndent()
        )

        val ktClass = PsiTreeUtil.findChildOfType(myFixture.file, KtClass::class.java)
        val nameIdentifier = ktClass!!.nameIdentifier!!
        val lineMarkerInfo = provider.getLineMarkerInfo(nameIdentifier)

        assertNotNull("Line marker should be created for Kotlin class with singular property", lineMarkerInfo)
        assertTrue("Tooltip should contain job name", lineMarkerInfo!!.lineMarkerTooltip!!.contains("singleKotlinJob"))
    }

    /**
     * Tests that NO line marker is created for a Kotlin class without @ConditionalOnProperty.
     */
    fun testNoLineMarkerForKotlinClassWithoutAnnotation() {
        myFixture.configureByText(
            "RegularClass.kt",
            """
            import org.springframework.context.annotation.Configuration

            @Configuration
            class RegularClass
            """.trimIndent()
        )

        val ktClass = PsiTreeUtil.findChildOfType(myFixture.file, KtClass::class.java)
        val lineMarkerInfo = provider.getLineMarkerInfo(ktClass!!.nameIdentifier!!)

        assertNull("Line marker should NOT be created for class without annotation", lineMarkerInfo)
    }
}