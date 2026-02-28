package com.github.mayleaf.springbatchrunner

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.kotlin.psi.KtClass

/**
 * Test Kotlin support for Spring Batch Runner.
 */
class KotlinSupportTest : BasePlatformTestCase() {

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
            @ConditionalOnProperty(name = ["spring.batch.job.names"], havingValue = "testJob")
            class TestBatchJob
            """.trimIndent()
        )

        val ktClass = PsiTreeUtil.findChildOfType(myFixture.file, KtClass::class.java)
        assertNotNull("Kotlin class should be found", ktClass)

        val identifier = ktClass!!.nameIdentifier
        assertNotNull("Class identifier should be found", identifier)

        // Test with the identifier (which is what LineMarkerProvider receives)
        val lineMarkerInfo = provider.getLineMarkerInfo(identifier!!)
        assertNotNull("Line marker should be created for Kotlin batch job class", lineMarkerInfo)
        assertTrue("Tooltip should contain job name", lineMarkerInfo!!.lineMarkerTooltip!!.contains("testJob"))
    }

    /**
     * Tests that a line marker is created for a Kotlin class with constant reference.
     */
    fun testLineMarkerForKotlinClassWithConstant() {
        myFixture.configureByText(
            "AudioBatchConfig.kt",
            """
            import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
            import org.springframework.context.annotation.Configuration

            object AudioGenerationBatchConfig {
                const val JOB_NAME = "audioGenerationJob"
            }

            @Configuration
            @ConditionalOnProperty(name = ["spring.batch.job.names"], havingValue = AudioGenerationBatchConfig.JOB_NAME)
            class AudioBatchJob
            """.trimIndent()
        )

        val classes = PsiTreeUtil.findChildrenOfType(myFixture.file, KtClass::class.java)
        val ktClass = classes.firstOrNull { it.name == "AudioBatchJob" }
        assertNotNull("Kotlin class AudioBatchJob should be found", ktClass)

        val identifier = ktClass!!.nameIdentifier
        assertNotNull("Class identifier should be found", identifier)

        val lineMarkerInfo = provider.getLineMarkerInfo(identifier!!)
        assertNotNull("Line marker should be created for Kotlin class with constant reference", lineMarkerInfo)
        assertTrue("Tooltip should contain job name", lineMarkerInfo!!.lineMarkerTooltip!!.contains("audioGenerationJob"))
    }
}
