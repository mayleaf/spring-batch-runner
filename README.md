# spring-batch-runner

![Build](https://github.com/mayleaf/spring-batch-runner/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/30410-spring-batch-runner.svg)](https://plugins.jetbrains.com/plugin/30410-spring-batch-runner)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/30410-spring-batch-runner.svg)](https://plugins.jetbrains.com/plugin/30410-spring-batch-runner)

<!-- Plugin description -->
**Spring Batch Runner** adds gutter run icons to Spring Batch job configuration classes, letting you launch batch jobs directly from the editor with a single click.

### How It Works

The plugin detects classes annotated with:

```java
@ConditionalOnProperty(name = "spring.batch.job.names", havingValue = "myJobName")
```

or the singular form `spring.batch.job.name`. When found, a **▶ Run** icon appears in the gutter next to the class name.

Clicking the icon:
1. Opens the **Edit Run Configuration** dialog pre-filled with:
   - The `--spring.batch.job.names=<jobName>` program argument
   - The detected Spring Boot main class
   - Any `@Value("#{jobParameters['...']}") `parameters found in the job's component classes
2. Runs the configuration after you confirm

### Features

- Works with both **Java** and **Kotlin** Spring Batch configurations
- Supports **Spring Batch 4 & 5** (`job.names` and `job.name` property keys)
- Detects job parameters from `@Value("#{jobParameters[...]}")` on fields, constructor parameters, and `@Bean` method parameters
- Auto-discovers the `@SpringBootApplication` main class in your module
- Resolves constant references in annotation attributes (static fields, companion object properties)
- Supports **K2 compiler** mode
<!-- Plugin description end -->

## Installation

- **IDE built-in plugin system:**

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "spring-batch-runner"</kbd> > <kbd>Install</kbd>

- **JetBrains Marketplace:**

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/30410-spring-batch-runner) and click <kbd>Install to ...</kbd> with your IDE open.

  Or download the [latest release](https://plugins.jetbrains.com/plugin/30410-spring-batch-runner/versions) and install manually via
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

- **Manually from GitHub:**

  Download the [latest release](https://github.com/mayleaf/spring-batch-runner/releases/latest) and install via
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## Usage

Given a Spring Batch configuration class like this:

```kotlin
@Configuration
@ConditionalOnProperty(name = "spring.batch.job.names", havingValue = "myJob")
class MyJobConfiguration {

    @Bean
    fun myStep(
        @Value("#{jobParameters['inputFile']}") inputFile: String,
        @Value("#{jobParameters['date']}") date: String,
    ): Step { ... }
}
```

A **▶** icon will appear in the gutter next to `MyJobConfiguration`. Click it to open a pre-configured run dialog with:

```
--spring.batch.job.names=myJob inputFile= date=
```

## Requirements

- IntelliJ IDEA 2025.2 or later (build 252+)
- Java or Kotlin Spring Boot project with Spring Batch

---

Plugin built with the [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template).
