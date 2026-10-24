# validate4j

YAML-driven, parallel form validation for Java 17+, with a Spring Boot starter.

Define your validation rules **outside the code**, in a YAML file. validate4j builds an immutable
validator per field at startup, validates independent fields **in parallel**, and returns every error
in a single, deterministic result.

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

```java
Validate4j v = Validate4j.builder().loadYamlResource("forms/signup.yaml").build();
ValidationResult result = v.validate("signup", Map.of("name", "A", "age", 17, "phone", "123"));
result.errors(); // [FieldError[field=name, code=string.minLength, message=Name must be 2-50 letters], ...]
```

## Modules

| Artifact | Use it when |
|----------|-------------|
| `io.github.sumeetg13:validate4j-core` | Plain Java. No framework dependencies, only SnakeYAML. |
| `io.github.sumeetg13:validate4j-spring-boot-starter` | Spring Boot 4.x. Auto-configures everything from `classpath:validate4j/*.yaml`. |

Gradle:

```kotlin
implementation("io.github.sumeetg13:validate4j-spring-boot-starter:0.1.0")
// or, without Spring:
implementation("io.github.sumeetg13:validate4j-core:0.1.0")
```

Maven:

```xml
<dependency>
    <groupId>io.github.sumeetg13</groupId>
    <artifactId>validate4j-spring-boot-starter</artifactId>
    <version>0.1.0</version>
</dependency>
```

## Spring Boot quickstart

1. Add the starter dependency.
2. Put your forms in `src/main/resources/validate4j/*.yaml`.
3. Annotate the request body:

```java
@PostMapping("/signup")
public Map<String, Object> signup(@ValidateWith("signup") @RequestBody Map<String, Object> body) {
    return Map.of("status", "created");
}
```

A failing request gets a `400` RFC 9457 problem response:

```json
{
  "type": "about:blank",
  "title": "Validation failed",
  "status": 400,
  "detail": "2 field(s) failed validation",
  "form": "signup",
  "errors": [
    { "field": "name", "code": "string.minLength", "message": "Name must be 2-50 letters" },
    { "field": "age",  "code": "number.min",       "message": "age must be at least 18" }
  ]
}
```

The body can be a `Map`, a record or a JavaBean. Inject `Validate4j` directly when you prefer to
inspect the `ValidationResult` yourself instead of throwing.

### Properties

| Property | Default | Meaning |
|----------|---------|---------|
| `validate4j.enabled` | `true` | Turn the auto-configuration off |
| `validate4j.locations` | `classpath*:validate4j/*.yaml`, `classpath*:validate4j/*.yml` | Where to find form files (`file:` works too) |
| `validate4j.parallel-threshold` | `8` | Forms with at least this many fields are validated in parallel |
| `validate4j.fail-on-missing-forms` | `false` | Fail startup when no form file is found |
| `validate4j.web-enabled` | `true` | Register `@ValidateWith` support and the ProblemDetail handler |
| `validate4j.hot-reload` | `false` | Watch form files on disk and apply edits without a restart |

Register a `Validate4jCustomizer` bean to set a custom `Executor` or add forms programmatically.

### Hot reload

With `validate4j.hot-reload=true`, validate4j watches the form files (using Java's `WatchService`)
and applies your changes as soon as you save. Files added to a watched directory are picked up too.

- The new forms are swapped in atomically. A request that is already being validated finishes with the old rules.
- If the edited file is invalid, the error is logged and the previous forms stay in use. Fix the file and save again.
- Only files on disk can be watched, not resources inside a jar. Point `validate4j.locations` at a
  `file:` path, or run with Gradle's `bootRun { sourceResources(sourceSets.main.get()) }` so edits under
  `src/main/resources` are seen (the sample app does this).
- On macOS the JDK polls for changes, so a save can take about 2 seconds to apply.

Without Spring:

```java
Validate4j v = Validate4j.builder()
        .loadYaml(Path.of("forms/signup.yaml"))
        .hotReload(true)
        .build();      // call v.close() to stop watching
```

`v.reload()` re-reads the files on demand.

## YAML reference

Every field takes `type` plus three reserved keys:

- `required` (default `false`): absent, `null` or blank values fail with code `required`. Optional fields skip validation when absent.
- `message`: overrides every message for the field. Placeholders: `{field}`, `{value}` and any option name such as `{minLength}`.
- All other keys are passed to the validator type. Unknown keys fail at startup, so typos never go unnoticed.

A file may hold many forms under `forms:` or one form with `form: <name>` at the top level. Multi-document YAML (`---`) works for both.

### Built-in types

| Type | Options | Error codes |
|------|---------|-------------|
| `string` | `minLength`, `maxLength`, `pattern` (whole-value regex), `trim` (default `true`) | `string.type`, `string.minLength`, `string.maxLength`, `string.pattern` |
| `number` | `min`, `max` (inclusive), `integer` (default `false`). Accepts numbers and numeric strings. | `number.type`, `number.integer`, `number.min`, `number.max` |
| `phone` | `digits` (one count or a list, e.g. `[10, 12]`), `allowPlusPrefix` (default `true`), `allowSeparators` (default `true`: space, `-`, `.`, `(`, `)`) | `phone.type`, `phone.format`, `phone.digits` |
| `email` | `maxLength` (default `254`) | `email.type`, `email.format`, `email.maxLength` |
| `enum` | `values` (required), `ignoreCase` (default `false`) | `enum.values` |
| `date` | `format` (`DateTimeFormatter` pattern, default `yyyy-MM-dd`, strict), `after`, `before` (exclusive; same format or `today`) | `date.type`, `date.format`, `date.after`, `date.before` |

## Custom validator types

Implement `ValidatorFactory` and return an immutable `Validator`:

```java
public final class UppercaseFactory implements ValidatorFactory {
    public String type() { return "uppercase"; }

    public Validator create(ValidatorConfig config) {
        config.allowKeys();                       // reject unknown YAML options
        return (field, value, ctx) -> value.toString().equals(value.toString().toUpperCase())
                ? List.of()
                : List.of(new FieldError(field, "uppercase", field + " must be upper case"));
    }
}
```

Register it one of three ways:

- Plain Java: `Validate4j.builder().register(new UppercaseFactory())`
- `ServiceLoader`: list the class in `META-INF/services/io.github.sumeetg13.validate4j.spi.ValidatorFactory`
- Spring Boot: declare it as a `@Bean`

Then use `type: uppercase` in YAML.

## How parallel validation works

Fields have no dependencies on each other, so each becomes a `CompletableFuture` on the configured
`Executor` (virtual threads on Java 21+, the common `ForkJoinPool` on 17). Results are merged in
declaration order, so output never depends on scheduling. Forms with fewer than `parallelThreshold`
fields run on the calling thread, since a thread hand-off costs more than a couple of regex checks.

## Building

```
./gradlew build                                  # all modules, 65 tests
./gradlew :examples:spring-boot-demo:bootRun     # sample app on :8080
curl -X POST localhost:8080/signup -H 'Content-Type: application/json' -d '{"name":"A","age":17}'
```

See `PLAN.md` for the roadmap (cross-field rules, i18n, JMH benchmarks) and `CONTRIBUTING.md` for how to add validator types.

## Publishing

Artifacts are published to Maven Central through the Sonatype Central Portal under the verified
namespace `io.github.sumeetg13`, using the [Gradle Maven Publish plugin](https://github.com/vanniktech/gradle-maven-publish-plugin).

One-time setup:

1. Generate a GPG key (`gpg --gen-key`) and upload the public key: `gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>`.
2. Export the private key for CI: `gpg --export-secret-keys --armor <KEY_ID>`.
3. In the Central Portal, open *View Account* and generate a **user token**.
4. Add these GitHub repository secrets:

| Secret | Value |
|--------|-------|
| `MAVEN_CENTRAL_USERNAME` | Central Portal token username |
| `MAVEN_CENTRAL_PASSWORD` | Central Portal token password |
| `GPG_SIGNING_KEY` | ASCII-armored private key from step 2 |
| `GPG_SIGNING_KEY_ID` | Key id or fingerprint in any form. The workflow trims it to the 8-character short id Gradle needs. |
| `GPG_SIGNING_PASSWORD` | Key passphrase |

Releasing: push a tag named after the version. The workflow takes the version from the tag, builds and
tests, uploads to the Central Portal, and releases automatically.

```
git tag v0.2.0
git push origin v0.2.0
```

The version in `build.gradle.kts` stays a `-SNAPSHOT` and never needs editing. The workflow refuses tags
that are not a version like `1.2.3` or `1.2.3-rc.1`, and versions that already exist on Maven Central.
You can also release from the Actions tab with *Run workflow*, entering the version by hand.

For local experiments, `./gradlew publishToMavenLocal` installs unsigned `0.2.0-SNAPSHOT` builds into `~/.m2`. Add `-PreleaseVersion=x.y.z` to build a specific version. The same
properties can be supplied locally in `~/.gradle/gradle.properties` (`mavenCentralUsername`, `mavenCentralPassword`,
`signingInMemoryKey`, `signingInMemoryKeyId`, `signingInMemoryKeyPassword`).

## License

Apache License 2.0. See `LICENSE`.
