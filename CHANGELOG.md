# Changelog

## Unreleased

### Added
- Hot reload: `Validate4j.Builder.hotReload(true)` / `validate4j.hot-reload=true` watch form files and apply edits without a restart. Invalid edits are logged and the previous forms stay in use.
- `Validate4j.reload()` to re-read form files on demand, and the `FormSource` abstraction behind it.

### Changed
- Releases now publish automatically: pushing a `vX.Y.Z` tag builds, uploads and releases that version on Maven Central.

## 0.1.0 - 2026-10-20

### Added
- Core engine: YAML-defined forms, immutable validators, parallel field validation with ordered results.
- Built-in types: `string`, `number`, `phone`, `email`, `enum`, `date`.
- `ValidatorFactory` SPI discovered via `ServiceLoader`.
- Spring Boot starter: auto-configuration, `@ValidateWith`, ProblemDetail (RFC 9457) error responses.
- Sample Spring Boot application under `examples/spring-boot-demo`.
