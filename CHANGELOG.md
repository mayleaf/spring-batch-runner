<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# spring-batch-runner Changelog

## [Unreleased]

## [1.0.4] - 2026-03-03

### Fixed

- Fixed description: Run icon is ▶ (play button), not leaf emoji

## [1.0.3] - 2026-03-03

### Changed

- Updated plugin icon to official Spring leaf logo style

## [1.0.2] - 2026-03-03

### Changed

- Renamed plugin to "Spring Batch Runner"

## [1.0.1] - 2026-03-03

### Added

- Support for resolving static field references in `@Value` annotations
  - Java: `@Value("#{jobParameters[" + JobConfig.PARAM + "]}")`
  - Kotlin: `@Value("#{jobParameters[$PARAM]}")` or `@Value("#{jobParameters[${Companion.PARAM}]}")`
- Resolve companion object properties and Java static final fields

### Changed

- Updated plugin description with 🍃 Spring leaf icon

## [1.0.0] - 2026-03-01

### Added

- Gutter run icons for Spring Batch job configuration classes annotated with `@ConditionalOnProperty(name = "spring.batch.job.names")`
- Auto-detection of `@SpringBootApplication` main class in the module
- Pre-filled run configuration with `--spring.batch.job.names=<jobName>` and detected `@Value("#{jobParameters[...]}")` parameters
- Support for Java and Kotlin Spring Batch configurations
- Support for Spring Batch 4 & 5 (`spring.batch.job.names` and `spring.batch.job.name`)
- K2 compiler mode support

[Unreleased]: https://github.com/mayleaf/spring-batch-runner/compare/1.0.4...HEAD
[1.0.4]: https://github.com/mayleaf/spring-batch-runner/compare/1.0.3...1.0.4
[1.0.3]: https://github.com/mayleaf/spring-batch-runner/compare/1.0.2...1.0.3
[1.0.2]: https://github.com/mayleaf/spring-batch-runner/compare/1.0.1...1.0.2
[1.0.1]: https://github.com/mayleaf/spring-batch-runner/compare/1.0.0...1.0.1
[1.0.0]: https://github.com/mayleaf/spring-batch-runner/commits/1.0.0
