package com.github.mayleaf.springbatchrunner

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Test cases for SpringBatchRunLineMarkerProvider.
 *
 * These tests verify that the line marker provider correctly identifies Spring Batch job
 * classes annotated with @ConditionalOnProperty and provides run icons in the gutter.
 */
class SpringBatchRunLineMarkerProviderTest : BasePlatformTestCase() {

    private lateinit var provider: SpringBatchRunLineMarkerProvider

    override fun setUp() {
        super.setUp()
        provider = SpringBatchRunLineMarkerProvider()
    }

    private fun findClass(className: String): PsiClass {
        val javaFile = myFixture.file as PsiJavaFile
        return javaFile.classes.firstOrNull { it.name == className }
            ?: PsiTreeUtil.findChildrenOfType(javaFile, PsiClass::class.java)
                .first { it.name == className }
    }

    /**
     * Tests that a line marker is created for a class with @ConditionalOnProperty
     * using literal string for both name and havingValue attributes (plural form).
     */
    fun testLineMarkerForSimpleConditionalOnPropertyWithPlural() {
        val javaFile = myFixture.configureByText(
            "TestBatchJob.java",
            """
            import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
            import org.springframework.context.annotation.Configuration;

            @Configuration
            @ConditionalOnProperty(name = "spring.batch.job.names", havingValue = "testJob")
            public class TestBatchJob {
            }
            """.trimIndent()
        )

        val psiClass = findClass("TestBatchJob")
        assertNotNull("Class should be found", psiClass)

        val identifier = psiClass.nameIdentifier
        assertNotNull("Class identifier should be found", identifier)

        val lineMarkerInfo = provider.getLineMarkerInfo(identifier!!)
        assertNotNull("Line marker should be created for batch job class", lineMarkerInfo)
        assertTrue("Tooltip should contain job name", lineMarkerInfo!!.lineMarkerTooltip!!.contains("testJob"))
    }

    /**
     * Tests that a line marker is created for a class with @ConditionalOnProperty
     * using the singular form "spring.batch.job.name".
     */
    fun testLineMarkerForSimpleConditionalOnPropertyWithSingular() {
        val javaFile = myFixture.configureByText(
            "TestBatchJob.java",
            """
            import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
            import org.springframework.context.annotation.Configuration;

            @Configuration
            @ConditionalOnProperty(name = "spring.batch.job.name", havingValue = "singleJob")
            public class TestBatchJob {
            }
            """.trimIndent()
        )

        val psiClass = findClass("TestBatchJob")
        assertNotNull("Class should be found", psiClass)

        val identifier = psiClass.nameIdentifier
        assertNotNull("Class identifier should be found", identifier)

        val lineMarkerInfo = provider.getLineMarkerInfo(identifier!!)
        assertNotNull("Line marker should be created for batch job class with singular property", lineMarkerInfo)
        assertTrue("Tooltip should contain job name", lineMarkerInfo!!.lineMarkerTooltip!!.contains("singleJob"))
    }

    /**
     * Tests that a line marker is created when using 'value' attribute instead of 'name'.
     */
    fun testLineMarkerWithValueAttribute() {
        val javaFile = myFixture.configureByText(
            "TestBatchJob.java",
            """
            import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
            import org.springframework.context.annotation.Configuration;

            @Configuration
            @ConditionalOnProperty(value = "spring.batch.job.names", havingValue = "valueJob")
            public class TestBatchJob {
            }
            """.trimIndent()
        )

        val psiClass = findClass("TestBatchJob")
        val lineMarkerInfo = provider.getLineMarkerInfo(psiClass.nameIdentifier!!)

        assertNotNull("Line marker should be created with 'value' attribute", lineMarkerInfo)
        assertTrue("Tooltip should contain job name", lineMarkerInfo!!.lineMarkerTooltip!!.contains("valueJob"))
    }

    /**
     * Tests that a line marker is created when name attribute is an array.
     */
    fun testLineMarkerWithArrayNameAttribute() {
        val javaFile = myFixture.configureByText(
            "TestBatchJob.java",
            """
            import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
            import org.springframework.context.annotation.Configuration;

            @Configuration
            @ConditionalOnProperty(name = {"spring.batch.job.names", "other.property"}, havingValue = "arrayJob")
            public class TestBatchJob {
            }
            """.trimIndent()
        )

        val psiClass = findClass("TestBatchJob")
        val lineMarkerInfo = provider.getLineMarkerInfo(psiClass.nameIdentifier!!)

        assertNotNull("Line marker should be created with array name attribute", lineMarkerInfo)
        assertTrue("Tooltip should contain job name", lineMarkerInfo!!.lineMarkerTooltip!!.contains("arrayJob"))
    }

    /**
     * Tests that a line marker is created when using a constant reference for the name attribute.
     */
    fun testLineMarkerWithConstantReference() {
        val javaFile = myFixture.configureByText(
            "TestBatchJob.java",
            """
            import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
            import org.springframework.context.annotation.Configuration;

            @Configuration
            @ConditionalOnProperty(name = BatchConstants.PROPERTY_NAME, havingValue = "constantJob")
            public class TestBatchJob {
                public static class BatchConstants {
                    public static final String PROPERTY_NAME = "spring.batch.job.names";
                }
            }
            """.trimIndent()
        )

        val psiClass = findClass("TestBatchJob")
        val lineMarkerInfo = provider.getLineMarkerInfo(psiClass.nameIdentifier!!)

        assertNotNull("Line marker should be created with constant reference", lineMarkerInfo)
        assertTrue("Tooltip should contain job name", lineMarkerInfo!!.lineMarkerTooltip!!.contains("constantJob"))
    }

    /**
     * Tests that a line marker is created when using a constant reference for the havingValue attribute.
     */
    fun testLineMarkerWithHavingValueConstantReference() {
        val javaFile = myFixture.configureByText(
            "TestBatchJob.java",
            """
            import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
            import org.springframework.context.annotation.Configuration;

            @Configuration
            @ConditionalOnProperty(name = "spring.batch.job.names", havingValue = BatchConstants.JOB_NAME)
            public class TestBatchJob {
                public static class BatchConstants {
                    public static final String JOB_NAME = "constantJobName";
                }
            }
            """.trimIndent()
        )

        val psiClass = findClass("TestBatchJob")
        val lineMarkerInfo = provider.getLineMarkerInfo(psiClass.nameIdentifier!!)

        assertNotNull("Line marker should be created with havingValue constant reference", lineMarkerInfo)
        assertTrue("Tooltip should contain job name", lineMarkerInfo!!.lineMarkerTooltip!!.contains("constantJobName"))
    }

    /**
     * Tests that a line marker is created for an inner class with @ConditionalOnProperty.
     */
    fun testLineMarkerForInnerClass() {
        val javaFile = myFixture.configureByText(
            "OuterClass.java",
            """
            import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
            import org.springframework.context.annotation.Configuration;

            @Configuration
            public class OuterClass {
                @Configuration
                @ConditionalOnProperty(name = "spring.batch.job.names", havingValue = "innerJob")
                public static class InnerBatchJob {
                }
            }
            """.trimIndent()
        )

        val innerClass = findClass("OuterClass.InnerBatchJob")
        assertNotNull("Inner class should be found", innerClass)

        val lineMarkerInfo = provider.getLineMarkerInfo(innerClass.nameIdentifier!!)
        assertNotNull("Line marker should be created for inner class", lineMarkerInfo)
        assertTrue("Tooltip should contain job name", lineMarkerInfo!!.lineMarkerTooltip!!.contains("innerJob"))
    }

    /**
     * Tests that a line marker is created when @ConditionalOnProperty is on the outer class
     * and the inner class is inspected.
     */
    fun testLineMarkerForInnerClassWithOuterAnnotation() {
        val javaFile = myFixture.configureByText(
            "OuterClass.java",
            """
            import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
            import org.springframework.context.annotation.Configuration;

            @Configuration
            @ConditionalOnProperty(name = "spring.batch.job.names", havingValue = "outerJob")
            public class OuterClass {
                @Configuration
                public static class InnerClass {
                }
            }
            """.trimIndent()
        )

        val innerClass = findClass("OuterClass.InnerClass")
        assertNotNull("Inner class should be found", innerClass)

        val lineMarkerInfo = provider.getLineMarkerInfo(innerClass.nameIdentifier!!)
        assertNotNull("Line marker should be created from outer class annotation", lineMarkerInfo)
        assertTrue("Tooltip should contain job name", lineMarkerInfo!!.lineMarkerTooltip!!.contains("outerJob"))
    }

    /**
     * Tests that NO line marker is created for a class without @ConditionalOnProperty annotation.
     */
    fun testNoLineMarkerForClassWithoutAnnotation() {
        val javaFile = myFixture.configureByText(
            "RegularClass.java",
            """
            import org.springframework.context.annotation.Configuration;

            @Configuration
            public class RegularClass {
            }
            """.trimIndent()
        )

        val psiClass = findClass("RegularClass")
        val lineMarkerInfo = provider.getLineMarkerInfo(psiClass.nameIdentifier!!)

        assertNull("Line marker should NOT be created for class without annotation", lineMarkerInfo)
    }

    /**
     * Tests that NO line marker is created when @ConditionalOnProperty has wrong property name.
     */
    fun testNoLineMarkerForWrongPropertyName() {
        val javaFile = myFixture.configureByText(
            "WrongPropertyClass.java",
            """
            import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
            import org.springframework.context.annotation.Configuration;

            @Configuration
            @ConditionalOnProperty(name = "some.other.property", havingValue = "wrongJob")
            public class WrongPropertyClass {
            }
            """.trimIndent()
        )

        val psiClass = findClass("WrongPropertyClass")
        val lineMarkerInfo = provider.getLineMarkerInfo(psiClass.nameIdentifier!!)

        assertNull("Line marker should NOT be created for wrong property name", lineMarkerInfo)
    }

    /**
     * Tests that NO line marker is created when havingValue is empty or missing.
     */
    fun testNoLineMarkerForEmptyHavingValue() {
        val javaFile = myFixture.configureByText(
            "EmptyValueClass.java",
            """
            import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
            import org.springframework.context.annotation.Configuration;

            @Configuration
            @ConditionalOnProperty(name = "spring.batch.job.names", havingValue = "")
            public class EmptyValueClass {
            }
            """.trimIndent()
        )

        val psiClass = findClass("EmptyValueClass")
        val lineMarkerInfo = provider.getLineMarkerInfo(psiClass.nameIdentifier!!)

        assertNull("Line marker should NOT be created for empty havingValue", lineMarkerInfo)
    }

    /**
     * Tests that NO line marker is created when havingValue is missing entirely.
     */
    fun testNoLineMarkerForMissingHavingValue() {
        val javaFile = myFixture.configureByText(
            "MissingValueClass.java",
            """
            import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
            import org.springframework.context.annotation.Configuration;

            @Configuration
            @ConditionalOnProperty(name = "spring.batch.job.names")
            public class MissingValueClass {
            }
            """.trimIndent()
        )

        val psiClass = findClass("MissingValueClass")
        val lineMarkerInfo = provider.getLineMarkerInfo(psiClass.nameIdentifier!!)

        assertNull("Line marker should NOT be created without havingValue", lineMarkerInfo)
    }

    /**
     * Tests that the line marker is NOT created for non-identifier elements.
     */
    fun testNoLineMarkerForNonIdentifierElement() {
        val javaFile = myFixture.configureByText(
            "TestClass.java",
            """
            import org.springframework.context.annotation.Configuration;

            @Configuration
            public class TestClass {
            }
            """.trimIndent()
        )

        val psiClass = findClass("TestClass")
        // Test with the class element itself (not the identifier)
        val lineMarkerInfo = provider.getLineMarkerInfo(psiClass)

        assertNull("Line marker should NOT be created for non-identifier element", lineMarkerInfo)
    }

    /**
     * Tests that a line marker is created when using a constant reference from a separate class.
     */
    fun testLineMarkerWithConstantFromSeparateClass() {
        myFixture.configureByText(
            "AudioGenerationBatchConfig.java",
            """
            public class AudioGenerationBatchConfig {
                public static final String JOB_NAME = "audioGenerationJob";
            }
            """.trimIndent()
        )

        val javaFile = myFixture.configureByText(
            "AudioBatchJob.java",
            """
            import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
            import org.springframework.context.annotation.Configuration;

            @Configuration
            @ConditionalOnProperty(name = {"spring.batch.job.names"}, havingValue = AudioGenerationBatchConfig.JOB_NAME)
            public class AudioBatchJob {
            }
            """.trimIndent()
        )

        val psiClass = findClass("AudioBatchJob")
        assertNotNull("Class should be found", psiClass)

        val identifier = psiClass.nameIdentifier
        assertNotNull("Class identifier should be found", identifier)

        val lineMarkerInfo = provider.getLineMarkerInfo(identifier!!)
        assertNotNull("Line marker should be created with constant reference from separate class", lineMarkerInfo)
        assertTrue("Tooltip should contain job name", lineMarkerInfo!!.lineMarkerTooltip!!.contains("audioGenerationJob"))
    }
}