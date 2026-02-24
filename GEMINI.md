# intellisql Development Guidelines

Auto-generated from all feature plans. Last updated: 2026-02-22

## Active Technologies

- Java 8+ (maintaining JDK 8 compatibility)

## Project Structure

```text
src/
tests/
```

## Commands

# Add commands for 

## Code Style

Java 8+ (maintaining JDK 8 compatibility): Follow standard conventions

## Recent Changes

- 001-sql-federation-translation: Added

<!-- MANUAL ADDITIONS START -->

## JDK 8 Compatibility Requirements

**This project MUST maintain JDK 8 compatibility.** All generated code must be compatible with Java 8.

### Code Restrictions

Do NOT use the following Java 9+ features:

- **No `var` keyword** (Java 10+) - always declare explicit types
- **No `List.of()`, `Set.of()`, `Map.of()`** (Java 9+) - use `Arrays.asList()`, `new ArrayList<>()`, `Collections.emptySet()`, etc.
- **No private interface methods** (Java 9+)
- **No modules (JPMS)** - no `module-info.java`
- **No try-with-resources with lambda** (Java 9+)
- **No diamond operator with anonymous classes** (Java 9+)
- **No `@SafeVarargs` on private methods** (Java 9+)
- **No multi-release JARs**
- **No `String::strip`, `String::isBlank`, `String::lines`** (Java 11+) - use `trim()`, custom checks
- **No `Optional::ifPresentOrElse`, `Optional::or`** (Java 9+)
- **No `Collection::toArray(IntFunction)`** (Java 11+)
- **No `InputStream::readAllBytes`, `InputStream::transferTo`** (Java 9+)

### Dependency Version Constraints

- **Mockito**: Use version 4.x (not 5.x - requires Java 11+)
- **JUnit Jupiter**: 5.10.x is compatible with Java 8
- **Check dependencies for Java 8 compatibility before adding**

### Correct Examples

```java
// Instead of var
String name = "test";  // NOT: var name = "test";

// Instead of List.of()
List<String> list = new ArrayList<>();  // NOT: List.of("a", "b")
list.add("a");
list.add("b");

// Or use Arrays.asList() for immutable list
List<String> list = Arrays.asList("a", "b");

// Instead of Map.of()
Map<String, String> map = new HashMap<>();  // NOT: Map.of("k", "v")
map.put("k", "v");
```

<!-- MANUAL ADDITIONS END -->
