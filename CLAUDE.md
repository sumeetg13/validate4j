# validate4j

YAML-driven form validation library for Java with a Spring Boot starter. See `PLAN.md` for the design.

## Build

```
./gradlew build          # compile + test all modules
./gradlew :validate4j-core:test
./gradlew :examples:spring-boot-demo:bootRun
```

## Conventions

- `validate4j-core` must not import anything from `org.springframework`. ArchUnit enforces it.
- Source compiles with `--release 17`. Do not use APIs newer than Java 17 in core except behind reflection (see `DefaultExecutors`).
- Validators are immutable and built once from `ValidatorConfig`. Reject unknown YAML keys in the factory.
- Never throw on a failed field; return `FieldError`s. Only the Spring advice converts results into exceptions.
- Error order must match field declaration order in YAML, even when validated in parallel.
- Every built-in validator gets a unit test covering: valid, each failing option, null/absent handling, and bad config.
