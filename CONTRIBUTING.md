# Contributing to IntelliSql

## Foundation

IntelliSql follows the Apache Software Foundation code of conduct and expects every contribution to reflect responsibility, professionalism, and respect for code quality.

## Core Principles

### 1. Care

Treat every contribution as crafted work.

- Every change must reflect thoughtful design and professional discipline.
- Code is expected to be a product of craftsmanship, not only an implementation artifact.
- Continuous improvement and attention to detail are mandatory.

Reason: high-quality software depends on sincere ownership and disciplined execution.

### 2. Readability

Code must express intent through reading alone.

- Prefer self-documenting code through clear naming and structure.
- Readers must understand the logic without stepping through a debugger.
- Split complex logic into well-named methods.

Reason: code is read far more often than it is written, and readability directly determines maintainability.

### 3. Cleanliness

Follow the spirit of clean code and refactoring.

- Apply clean code principles consistently.
- Refactor when quality declines.
- Pay technical debt down promptly.

Reason: clean code reduces defects, improves maintainability, and keeps delivery fast.

### 4. Consistency

Keep style, naming, and usage patterns aligned across the project.

- Follow the same formatting and naming conventions everywhere.
- Solve similar problems in similar ways.
- Maintain consistency in naming, file layout, error handling, and logging.

Reason: consistency lowers cognitive load and speeds up comprehension.

### 5. Simplicity

Express the right intent with the least necessary code.

- Remove dead code promptly.
- Every line must have a clear purpose.
- DRY is mandatory.
- Repeated configuration is treated as duplication and must be eliminated.

Reason: less code yields fewer defects, easier maintenance, and clearer understanding.

### 6. Abstraction

Keep responsibilities and abstraction levels clear.

- Each layer must carry a single responsibility.
- Concepts must be extracted and named correctly.
- Do not mix abstraction levels inside the same class or method.

Reason: sound abstraction makes complex systems easier to reason about.

### 7. Excellence

Every character, statement, and space must justify its existence.

- Placeholder code and untracked `TODO` items are prohibited.
- Remove unnecessary imports, variables, and statements.
- Reviews should challenge anything that lacks clear value.

Reason: excellence in small details accumulates into excellence across the system.

## Coding Standards

### Formatting Rules

- Use LF line endings.
- Lines up to 200 characters may stay on one line.
- Avoid meaningless blank lines.
- Do not separate statements inside a method body with cosmetic blank lines.
- Do not add blank lines between variable declarations and the following code.
- Do not use blank lines to separate logic blocks; extract private methods instead.
- Use spinal-case for configuration file names.

Example:

```java
// Incorrect: unnecessary blank line
@Test
void assertConfigParsing() {
    String yaml = Files.readString(Paths.get("conf/model.yaml"));
    ModelConfig config = ConfigLoader.parse(yaml);

    assertThat(config.getDataSources(), hasSize(2));
    assertThat(config.getDataSources().get(0).getType(), is(DataSourceType.MYSQL));
}

// Correct: continuous code without cosmetic blank lines
@Test
void assertConfigParsing() {
    String yaml = Files.readString(Paths.get("conf/model.yaml"));
    ModelConfig config = ConfigLoader.parse(yaml);
    assertThat(config.getDataSources(), hasSize(2));
    assertThat(config.getDataSources().get(0).getType(), is(DataSourceType.MYSQL));
}
```

### Naming Rules

General rules:

- Names must be self-explanatory.
- Avoid abbreviations in class and method names.
- Limited abbreviations are acceptable for some variable names.

Standard abbreviations:

- `arguments` -> `args`
- `parameters` -> `params`
- `environment` -> `env`
- `properties` -> `props`
- `configuration` -> `config`

Proper noun abbreviations:

- Acronyms with up to three characters stay uppercase, for example `SQL92Lexer`, `XMLTransfer`, `MySQLAdminExecutorCreator`.
- Acronyms longer than three characters use camel-case style, for example `JdbcUrlAppender`, `YamlAgentConfigurationSwapper`.
- Variables always use lower camel case, for example `mysqlAuthenticationMethod`, `sqlStatement`, `mysqlConfig`.

Local variable rules:

- Use `result` for returned local variables unless directly returning a method parameter.
- Use `each` as the loop variable in collection iteration.
- Use `entry` for map entries.
- Name caught exceptions `ex`.
- Name intentionally ignored exceptions `ignored`.
- Method parameters must not be named `result`, `each`, or `entry`.
- Utility classes should use the `XxxUtils` pattern.

Condition rules:

- For `equals` and `==`, place constants on the left and variables on the right.
- For greater-than or less-than comparisons, place variables on the left and constants on the right.

### Code Style Rules

- Avoid `this` unless it disambiguates constructor parameters or fields.
- Local variables should not be declared `final`.
- Prefer `final` classes unless inheritance is required.
- Extract nested loops into methods when practical.
- Keep field declaration order and parameter order consistent across related classes and methods.
- Prefer guard clauses.
- Use the narrowest reasonable visibility for classes and methods.
- Keep private helper methods immediately after the methods that use them, in the same appearance order.
- Method parameters and return values must not be `null`.
- Do not use `Optional` as a method parameter; pass ordinary values instead. `null` is allowed only when required by the API contract.
- Prefer Lombok for constructors, accessors, and logger fields.
- Import class names instead of inlining fully qualified names.
- Prefer `LinkedList`; use `ArrayList` only when indexed access is required.
- Initialize collection capacities for `ArrayList`, `HashMap`, and similar resizeable collections.
- Prefer ternary operators for simple return and assignment branches.
- Keep ternary expressions flat.
- Prefer positive semantics in conditions.

### Performance Annotation

Use `@HighFrequencyInvocation` to mark code paths that require performance-focused implementation.

Apply it when:

- A class, method, or constructor sits on a frequently executed request path.
- `canBeCached = true` identifies reusable cached resources such as database connections.

Within annotated code, avoid:

- Java Stream API
- String concatenation through `+`
- `LinkedList#get(int index)`

### Comments and Logging

- Write logs and comments in English.
- Only `JAVADOC`, `TODO`, and `FIXME` comments are allowed.
- Public classes and methods must include `JAVADOC`.
- Public API and SPI documentation must be clear and complete.
- Other classes, internal methods, and overridden methods do not require `JAVADOC`.
- Prefer extracting small methods over explaining code with inline comments.

## Testing Standards

### AIR Principles

Automatic:

- Unit tests must run automatically.
- Manual output inspection is prohibited.
- Use assertions instead of `System.out` or logs for verification.

Independent:

- Test cases must not call one another.
- Test execution order must not matter.
- Every unit test must run independently.

Repeatable:

- Tests must not depend on the external environment.
- Tests must be repeatable.

### BCDE Principles

- Border: cover loop boundaries, special values, and data-order edge cases.
- Correct: verify expected behavior with valid input.
- Design: shape tests in a way that supports production design quality.
- Error: verify expected behavior for invalid data and exceptional flows.

### Test Naming and Structure

- Name all test cases with the `assert` prefix.
- Verify behavior through public APIs only.
- Do not call private members through reflection.
- If reflection access to fields is required, use `Plugins.getMemberAccessor()`.
- If one production method is covered by exactly one test, use `assert<MethodName>` without a suffix.
- Use one test method per public production method when practical.
- Keep test method order aligned with production method order when practical.
- Parameterized tests must provide display names through arguments and use `"{0}"` as the display-name template.
- Keep assertions precise; minimize fuzzy assertions such as `containsString`.
- Separate setup code from verification code.
- Limit static imports to Mockito, JUnit assertions, Hamcrest `CoreMatchers`, and `MatcherAssert`.

### Assertion Rules

- Use `assertTrue` and `assertFalse` for booleans.
- Use `assertNull` and `assertNotNull` for nullability.
- Use `assertThat(actual, is(expected))` instead of `assertEquals`.
- Use `assertThat(..., isA(...))` instead of `instanceOf`.
- Replace `assertSame` and `assertNotSame` with value-based assertions.
- Use Hamcrest matchers such as `is()` and `not()` for precise and readable assertions.
- Name actual values `actualXxx`.
- Name expected values `expectedXxx`.
- Test classes and `@Test` methods do not require `JAVADOC`.

### Mocking Rules

Use mocks when:

- A unit test would otherwise need to connect to an environment.
- A unit test depends on objects that are expensive or irrelevant to construct, such as deeply nested unrelated graphs.

For static methods and constructors:

- Prefer `AutoMockExtension` and `StaticMockSettings` from the test framework so resources are released automatically.
- If using Mockito `mockStatic` or `mockConstruction`, close them with try-with-resources or explicit cleanup.

Additional rules:

- If verifying a single invocation, use `verify(...)` directly instead of `times(1)`.
- Use Mockito `RETURNS_DEEP_STUBS` for deep chained interactions instead of manual layer-by-layer mocking.
- Use standardized prefixes such as `foo_` and `bar_` for test data.
- Use `PropertiesBuilder` to simplify `Properties` construction.

## Technical Constraints

### JDK Compatibility

- Backend code must use JDK 8 syntax.
- Keep compatibility with JDK 8 as the minimum supported version.

### Enforced Style

- Backend code must comply with ShardingSphere `spotless` and `checkstyle`.
- Java code must stay free of useless blank lines.
- Naming must remain easy to understand.
- Preserve elegance through reasonable class and method decomposition.

### Architecture

- Project modules follow the multi-module direction used by 360 QuickSql.
- Design for extensibility and maintainability.

### Security

- Use the latest release versions of dependencies.
- Keep the project free of known CVE vulnerabilities.

## Submission Requirements

### Build Process

Before submitting code:

1. Follow all coding standards.
2. Ensure the full build process succeeds:
   - Apache license header checks
   - Checkstyle
   - Compilation
   - Unit tests
3. Run `./mvnw clean install -B -T1C -Pcheck`.
4. Run `./mvnw spotless:apply -Pcheck`.

### Coverage

- Keep coverage at least as high as the `master` branch.
- Except for trivial getters and setters, unit tests should cover production code fully.

### Commit Guidance

- Split design changes carefully.
- Prefer small, focused commits.
- Preserve the completeness of each commit.

### IDEA Configuration

- Import `src/resources/idea/code-style.xml` in IntelliJ IDEA to align formatting.
- Import `src/resources/idea/inspections.xml` in IntelliJ IDEA to detect potential issues early.

## Governance

### Amendment Process

1. Constitution changes must record:
   - The exact sections changed
   - The reason for the change
   - A migration plan for existing code when applicable
2. Every pull request and review must validate compliance with this document.
3. Any introduced complexity must be justified against the principle of simplicity.

### Versioning Policy

- MAJOR: incompatible governance changes, removals, or principle redefinitions
- MINOR: new principles, new sections, or substantial guidance expansion
- PATCH: clarifications, wording fixes, and non-semantic improvements

### Compliance Review

- Every code review must include a compliance check against this document.
- Violations must be fixed or explicitly justified before merge.
- Complexity justifications must reference the concrete alternatives that were rejected.

Version: 1.0.1  
Approved: 2026-02-16  
Last Revised: 2026-02-17
