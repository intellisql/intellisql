# IntelliSql Codex Guidelines

Derived from `CLAUDE.md` and `GEMINI.md`. Last updated: 2026-03-20

## Active Technologies

- Java 8 (source and target must remain `1.8`)
- Maven multi-module build
- Apache Calcite and Avatica
- HikariCP, Jackson, SLF4J, Logback
- JUnit 5, AssertJ, Mockito, Testcontainers

## Project Structure

```text
intellisql-common/        Shared utilities, config, metadata
intellisql-parser/        SQL parsing based on Calcite
intellisql-features/      Federation, translator, optimizer modules
intellisql-connector/     Data source connectors
intellisql-jdbc/          JDBC driver
intellisql-server/        Server runtime and packaged config
intellisql-client/        CLI client
intellisql-distribution/  Server and client distribution packaging
intellisql-test/          Integration and end-to-end tests
src/resources/            Spotless, Checkstyle, and shared build config
```

## Commands

```bash
# Full build without tests
./mvnw clean install -DskipTests

# Run unit tests
./mvnw test

# Run integration tests
./mvnw verify -DskipUnitTests

# Run all tests
./mvnw verify

# Run checks used before submission
./mvnw clean verify -Pcheck

# Format code
./mvnw spotless:apply

# Check style only
./mvnw checkstyle:check

# Test only the affected module and its dependencies
./mvnw -pl <module> -am test
```

## Working Rules For Codex

- Keep changes scoped to the smallest affected module.
- Prefer targeted validation first, then broader verification when build logic, shared modules, or dependencies change.
- Preserve Apache license headers in Java source files.
- Keep Maven source/target at Java 8 and preserve existing profiles such as `check` and `jdk9-plus`.
- Do not introduce new dependencies or APIs without confirming Java 8 compatibility.
- Integration and end-to-end tests under `intellisql-test` use Testcontainers and usually require Docker.
- When changing formatting-sensitive files, rely on Spotless and existing Checkstyle rules rather than manual style exceptions.

## Code Style

- Follow Spotless formatting and Checkstyle rules from `src/resources/spotless` and `src/resources/checkstyle`.
- Use explicit types; avoid clever shorthand that hurts readability.
- Keep import groups ordered as documented in `CONTRIBUTING.md`.
- Always use braces, even for single-line conditionals or loops.
- Avoid multiple consecutive blank lines and trailing whitespace.
- Prefer descriptive test names such as `shouldReturnEmptyListWhenNoResults`.

## Testing Guidance

- Use JUnit Jupiter for tests.
- Use AssertJ for assertions when expressive assertions help readability.
- Use Mockito for mocking; align with the existing Mockito 4.11.0 setup.
- For connector, server, or cross-source changes, add or update integration coverage where practical.
- For parser, translator, optimizer, client, or utility changes, add focused unit tests near the touched code.

<!-- MANUAL ADDITIONS START -->

## JDK 8 Compatibility Requirements

**This project must remain compatible with JDK 8.** Generated or modified code must compile and run on Java 8 unless the project maintainers explicitly change the baseline.

### Forbidden Java 9+ Features

- No `var`
- No `List.of()`, `Set.of()`, or `Map.of()`
- No private interface methods
- No `module-info.java`
- No `String.strip()`, `String.isBlank()`, or `String.lines()`
- No `Optional.ifPresentOrElse()` or `Optional.or()`
- No `InputStream.readAllBytes()` or `InputStream.transferTo()`
- No `Collection.toArray(IntFunction)`
- No diamond operator with anonymous inner classes

### Dependency Constraints

- Keep `junit.version` aligned with the current Java 8 compatible setup: `5.8.2`
- Keep `mockito.version` aligned with the current Java 8 compatible setup: `4.11.0`
- Elasticsearch support currently uses the 7.x client line for Java 8 compatibility
- Check Java baseline compatibility before upgrading any library

### Preferred Java 8 Equivalents

```java
String name = "test";

List<String> values = new ArrayList<String>();
values.add("a");
values.add("b");

List<String> fixedValues = Arrays.asList("a", "b");

Map<String, String> mapping = new HashMap<String, String>();
mapping.put("k", "v");
```

<!-- MANUAL ADDITIONS END -->
