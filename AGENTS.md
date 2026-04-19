# IntelliSQL Agent Guidelines

This file is the shared repository instruction entry for coding agents.

## Scope

- Apply these rules to code generation, code modification, refactoring, testing, review comments, documentation, and logs.
- Read [`CONTRIBUTING.md`](./CONTRIBUTING.md) before making or reviewing code changes.
- Treat [`CONTRIBUTING.md`](./CONTRIBUTING.md) as the source of truth when any instruction differs.

## Required Workflow

1. Read [`CONTRIBUTING.md`](./CONTRIBUTING.md) before changing code.
2. Inspect the touched module and existing implementation before writing code.
3. Generate code that follows IntelliSQL naming, formatting, abstraction, logging, and testing rules.
4. Re-check the final diff against [`CONTRIBUTING.md`](./CONTRIBUTING.md) before finishing.

## Repository Baseline

- Java baseline: JDK 8. Source and target compatibility must remain `1.8`.
- Build system: Maven multi-module project.
- Main modules:
  - `intellisql-common`
  - `intellisql-parser`
  - `intellisql-features`
  - `intellisql-connector`
  - `intellisql-jdbc`
  - `intellisql-server`
  - `intellisql-client`
  - `intellisql-distributions`
  - `intellisql-tests`

## Build And Validation

```bash
./mvnw test
./mvnw verify
./mvnw clean verify -Pcheck
./mvnw spotless:apply
./mvnw checkstyle:check
./mvnw -pl <module> -am test
```

- Prefer targeted validation first.
- Run broader verification when touching shared modules, build logic, or cross-module contracts.
- Integration and end-to-end tests under `intellisql-tests` may require Docker through Testcontainers.

## IntelliSQL Coding Requirements

- Keep JDK 8 compatibility.
- Follow IntelliSQL principles: care, readability, cleanliness, consistency, simplicity, abstraction, and excellence.
- Keep methods and classes at a consistent abstraction level.
- Use self-explanatory names.
- Keep changes scoped to the smallest reasonable module.
- Preserve existing license headers and project layout conventions.

## Formatting And Structure

- Use LF line endings.
- Avoid meaningless blank lines.
- Do not add cosmetic blank lines inside method bodies.
- Do not separate logic blocks with blank lines. Extract private methods instead.
- Do not add blank lines between variable declarations and the following code.
- Use spinal-case for configuration file names.
- Prefer existing Spotless and Checkstyle rules over ad hoc formatting.

## Naming And Style

- Prefer clear names over abbreviations.
- Use approved abbreviations from [`CONTRIBUTING.md`](./CONTRIBUTING.md) only.
- Use `result` for returned local variables when needed.
- Use `each` for loop variables and `entry` for map entries.
- Name caught exceptions `ex` and intentionally ignored exceptions `ignored`.
- Avoid unnecessary `this`.
- Prefer `final` classes unless inheritance is required.
- Prefer guard clauses.
- Keep helper methods immediately after their callers.
- Use the narrowest reasonable visibility.
- Do not use `Optional` as a method parameter.
- Keep parameters and return values non-null unless the API contract explicitly requires otherwise.
- Prefer Lombok where the codebase already uses it for constructors, accessors, and loggers.

## Java 8 Guardrails

Do not introduce Java 9+ APIs or syntax, including:

- `var`
- `List.of`, `Set.of`, `Map.of`
- private interface methods
- `module-info.java`
- `String.strip`, `String.isBlank`, `String.lines`
- `Optional.ifPresentOrElse`, `Optional.or`
- `InputStream.readAllBytes`, `InputStream.transferTo`
- `Collection.toArray(IntFunction)`

Keep dependency upgrades compatible with Java 8. Mockito 4.x and the current JUnit Jupiter line in the project are valid baselines.

## Comments, Logs, And Documentation

- Write comments and logs in English.
- Only `JAVADOC`, `TODO`, and `FIXME` comments are allowed.
- Public classes and public methods must include `JAVADOC`.
- Prefer extracting small methods over adding inline explanatory comments.

## Testing Requirements

- Test names must start with `assert`.
- Verify behavior through public APIs.
- Keep tests automatic, independent, and repeatable.
- Use precise assertions.
- Use `assertTrue` and `assertFalse` for booleans.
- Use `assertNull` and `assertNotNull` for null checks.
- Prefer `assertThat(actual, is(expected))` style assertions over `assertEquals`.
- Use mocks when environment dependencies or expensive irrelevant collaborators would otherwise be required.
- Close static and construction mocks safely.

## Conflict Resolution

- Follow [`CONTRIBUTING.md`](./CONTRIBUTING.md) when this file is less specific.
- Follow module-local conventions when they are stricter and remain compatible with [`CONTRIBUTING.md`](./CONTRIBUTING.md).
