# spring-batch-runner

![Build](https://github.com/mayleaf/spring-batch-runner/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/30410-spring-batch-runner.svg)](https://plugins.jetbrains.com/plugin/30410-spring-batch-runner)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/30410-spring-batch-runner.svg)](https://plugins.jetbrains.com/plugin/30410-spring-batch-runner)

<!-- Plugin description -->
**Run Spring Batch jobs with a single click.** This plugin adds ▶ Run icons to your batch job configuration classes—no more manually typing `--spring.batch.job.names=...` every time.

### Quick Start

Just annotate your job configuration with `@ConditionalOnProperty`:

```java
@Configuration
@ConditionalOnProperty(name = "spring.batch.job.names", havingValue = "myJob")
public class MyJobConfig { ... }
```

A **▶ Run** icon appears in the gutter. Click it to launch your batch job instantly.

### What It Does

- **Auto-fills run configuration** with `--spring.batch.job.names=<jobName>`
- **Detects job parameters** from `@Value("#{jobParameters['paramName']}")`—no need to remember parameter names
- **Finds your main class** automatically (looks for `@SpringBootApplication`)

### Supported

| | |
|---|---|
| **Languages** | Java, Kotlin |
| **Spring Batch** | 4.x, 5.x (`job.names` and `job.name`) |
| **Kotlin** | K2 compiler mode supported |
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
