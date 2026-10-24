# validate4j — Implementation Plan

Dynamic, YAML-driven form validation for Java, with a Spring Boot starter.
Field rules live outside the code; each field is validated by an immutable
`Validator` built once at startup, and independent fields run in parallel.

## Goals

- Rules defined in YAML, not annotations, so they can change without a rebuild.
- Core module has zero Spring dependency. Anyone on plain Java 17+ can use it.
- Pluggable validators: ship a useful set, let users add their own via `ServiceLoader` or Spring beans.
- Parallel validation of independent fields, with a sequential fallback for small forms.
- Deterministic output: errors come back in the order fields are declared in YAML.
- Open source friendly: Apache 2.0, CI, Maven Central publishing, sample app.

## Non-goals for 1.0

- Cross-field rules (planned for 1.1 as a second phase that runs after field checks).
- Bean Validation (JSR 380) interop.

## Module layout

```
validate4j/
├── validate4j-core/                 # engine, built-ins, YAML loader (no Spring)
├── validate4j-spring-boot-starter/  # auto-config, @ValidateWith, ProblemDetail advice
├── examples/spring-boot-demo/       # runnable sample
├── PLAN.md
└── CLAUDE.md
```

## Core design

| Type | Role |
|------|------|
| `Validator` | `List<FieldError> validate(String field, Object value, ValidationContext ctx)` |
| `ValidatorFactory` | SPI: `type()` + `create(ValidatorConfig)`; discovered via `ServiceLoader` |
| `ValidatorConfig` | Typed, key-checked view of a YAML block; rejects unknown keys |
| `FieldRule` | field name, `required`, custom `message`, the built `Validator` |
| `FormDefinition` | form name + ordered `FieldRule`s |
| `FormValidator` | validates a `Map<String,Object>` against a `FormDefinition` |
| `ValidationResult` / `FieldError` | immutable result; never throws on a failed field |
| `YamlFormLoader` | parses one or many forms from YAML, fails fast on bad config |
| `Validate4j` | facade/builder: registry + executor + loaded forms |

### Execution

- `ParallelExecution`: one `CompletableFuture` per field, joined with `allOf`, merged in declaration order.
- Default executor: virtual-thread-per-task when running on Java 21+, otherwise `ForkJoinPool.commonPool()`. User can inject any `Executor`.
- `parallelThreshold` (default 8): forms with fewer fields run sequentially because thread handoff costs more than a regex check.

### YAML format

```yaml
forms:
  signup:
    fields:
      name:
        type: string
        required: true
        minLength: 2
        maxLength: 50
        pattern: "^[A-Za-z ]+$"
        message: "Name must be 2-50 letters"
      age:
        type: number
        min: 18
        max: 120
      phone:
        type: phone
        digits: [10, 12]
```

A file may also hold a single form at the top level (`form: signup` + `fields:`).

### Built-in validators (type → options)

- `string` → `minLength`, `maxLength`, `pattern`, `trim`
- `number` → `min`, `max`, `integer`
- `phone` → `digits` (list of allowed counts), `allowPlusPrefix`
- `email` → RFC-ish pattern, `maxLength`
- `enum` → `values`, `ignoreCase`
- `date` → `format`, `before`, `after`

Messages support `{field}`, `{value}` and any option name such as `{min}`.

## Spring Boot starter

- `Validate4jAutoConfiguration` builds the `Validate4j` bean from `classpath*:validate4j/*.yaml` (configurable via `validate4j.locations`).
- Any `ValidatorFactory` Spring bean is registered automatically.
- `@ValidateWith("signup")` on a `@RequestBody Map` parameter triggers validation through a `RequestBodyAdvice`.
- `FormValidationException` → `ProblemDetail` 400 via `@RestControllerAdvice`.
- Properties: `validate4j.enabled`, `validate4j.locations`, `validate4j.parallel-threshold`.

## Phases

1. **Core skeleton** — interfaces, result model, config view, registry, YAML loader. ✅
2. **Built-in validators** — string, number, phone, email, enum, date, each unit-tested. ✅
3. **Parallel engine** — executor strategy, threshold, ordering guarantee, tests. ✅
4. **Spring Boot starter** — auto-config, annotation, advice, slice tests. ✅
5. **OSS polish** — README, LICENSE, CONTRIBUTING, CI, sample app, publishing config. ✅ (publishing creds left to the owner)

## Hot reload

- Opt-in (`hotReload(true)` / `validate4j.hot-reload`). Validators stay immutable; a reload builds a
  whole new form map from every `FormSource` and swaps it in with one volatile write.
- `FormFileWatcher` uses `WatchService` on the parent directory, debounces bursts of events, and
  runs reloads on one daemon thread. A failed reload keeps the previous forms.

## Later

- Cross-field rules (`rules:` block evaluated after field phase).
- POJO input via a `FieldAccessor` abstraction (reflection-based in the starter).
- i18n via Spring `MessageSource`.
- JMH benchmark module to tune `parallelThreshold`.

## Toolchain

- Gradle 9.x (wrapper committed), Kotlin DSL, version catalog.
- Published to Maven Central (Sonatype Central Portal) under `io.github.sumeetg13` via the Gradle Maven Publish plugin.
- Compiled with `--release 17`; CI tests on JDK 17 and 21.
- JUnit 5, AssertJ, ArchUnit (enforces core never imports Spring).
- Spring Boot 4.1.x for the starter.
