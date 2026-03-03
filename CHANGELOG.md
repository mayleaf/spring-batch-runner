<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# spring-batch-runner Changelog

## [Unreleased]

## [1.0.0] - 2026-03-01

### Added

- Gutter run icons for Spring Batch job configuration classes annotated with `@ConditionalOnProperty(name = "spring.batch.job.names")`
- Auto-detection of `@SpringBootApplication` main class in the module
- Pre-filled run configuration with `--spring.batch.job.names=<jobName>` and detected `@Value("#{jobParameters[...]}")` parameters
- Support for Java and Kotlin Spring Batch configurations
- Support for Spring Batch 4 & 5 (`spring.batch.job.names` and `spring.batch.job.name`)
- K2 compiler mode support

[Unreleased]: https://github.com/mayleaf/spring-batch-runner/compare/1.0.0...HEAD
[1.0.0]: https://github.com/mayleaf/spring-batch-runner/commits/1.0.0
