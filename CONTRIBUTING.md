# Contributing to validate4j

Thanks for helping. Bug reports, docs fixes and new validator types are all welcome.

## Development

```
./gradlew build                      # everything
./gradlew :validate4j-core:test      # just the core
./gradlew :examples:spring-boot-demo:bootRun
```

Requirements: JDK 17 or newer. The wrapper downloads Gradle.

## Adding a built-in validator type

1. Create `validate4j-core/src/main/java/io/github/validate4j/validators/FooValidator.java` with a nested `Factory`.
2. Call `config.allowKeys(...)` first thing in the factory so typos fail at startup.
3. Register the factory in `validate4j-core/src/main/resources/META-INF/services/io.github.sumeetg13.validate4j.spi.ValidatorFactory`.
4. Add a test covering: a valid value, each failing option, non-text input, and invalid configuration.
5. Document the type and its options in `README.md`.

## Pull requests

- Keep `validate4j-core` free of Spring. ArchUnit will fail the build otherwise.
- Code compiles with `--release 17`; do not use newer APIs in core.
- Run `./gradlew build` before pushing.
- Describe the behaviour change in `CHANGELOG.md` under *Unreleased*.
