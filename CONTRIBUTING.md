# Contributing to IntelliSql

Thank you for your interest in contributing to IntelliSql! This document provides guidelines and instructions for contributing.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Code Style](#code-style)
- [Commit Guidelines](#commit-guidelines)
- [Pull Request Process](#pull-request-process)
- [Testing Guidelines](#testing-guidelines)

## Code of Conduct

By participating in this project, you agree to maintain a respectful and inclusive environment. Please be considerate of others and follow standard open-source community guidelines.

## Getting Started

1. Fork the repository
2. Clone your fork:
   ```bash
   git clone https://github.com/YOUR_USERNAME/intellisql.git
   cd intellisql
   ```
3. Add upstream remote:
   ```bash
   git remote add upstream https://github.com/intellisql/intellisql.git
   ```
4. Create a feature branch:
   ```bash
   git checkout -b feature/your-feature-name
   ```

## Development Setup

### Prerequisites

- Java 8 or higher (JDK 8 recommended for compatibility)
- Maven 3.6+
- Git
- IDE (IntelliJ IDEA recommended)

### Build the Project

```bash
./mvnw clean install -DskipTests
```

### IDE Configuration

#### IntelliJ IDEA

1. Import the project as a Maven project
2. Enable annotation processing:
   - Settings -> Build, Execution, Deployment -> Compiler -> Annotation Processors
   - Enable annotation processing
3. Install Lombok plugin:
   - Settings -> Plugins -> Search "Lombok" -> Install
4. Set code style:
   - Settings -> Editor -> Code Style -> Java
   - Set indent to 4 spaces

## Code Style

IntelliSql follows strict code style guidelines based on ShardingSphere's conventions.

### Formatting

We use Spotless with Palantir Java Format for code formatting:

```bash
# Format all code
./mvnw spotless:apply

# Check formatting
./mvnw spotless:check
```

### Checkstyle

We use Checkstyle for code quality checks:

```bash
# Run checkstyle
./mvnw checkstyle:check
```

### Key Style Rules

#### Import Order

Imports should be ordered as follows:
1. `org.intellisql.*`
2. `org.apache.*`
3. Other imports (alphabetical)
4. `javax.*`
5. `java.*`
6. Static imports

```java
import org.intellisql.parser.SqlParser;
import org.intellisql.optimizer.QueryOptimizer;

import org.apache.calcite.sql.SqlNode;

import com.google.common.collect.Lists;

import javax.annotation.Nullable;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
```

#### Naming Conventions

- **Classes**: PascalCase (e.g., `QueryExecutor`, `MySQLConnector`)
- **Methods**: camelCase (e.g., `executeQuery`, `parseStatement`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `MAX_CONNECTIONS`, `DEFAULT_TIMEOUT`)
- **Variables**: camelCase (e.g., `connectionPool`, `resultSet`)

#### Blank Lines

- One blank line between methods
- One blank line between import groups
- No trailing whitespace
- No multiple consecutive blank lines

#### Braces

Always use braces, even for single-line statements:

```java
// Good
if (condition) {
    doSomething();
}

// Bad
if (condition)
    doSomething();
```

#### License Header

All Java files must include the Apache License header:

```java
/*
 * Licensed to the IntelliSql Project under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The IntelliSql Project licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
```

## Commit Guidelines

### Commit Message Format

We follow a conventional commit format:

```
<type>(<scope>): <subject>

<body>

<footer>
```

#### Types

- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, no code change)
- `refactor`: Code refactoring
- `test`: Adding or modifying tests
- `chore`: Build process or auxiliary tool changes
- `perf`: Performance improvements

#### Scope

The module or component affected:
- `parser`
- `optimizer`
- `executor`
- `connector`
- `jdbc`
- `server`
- `client`
- `kernel`

#### Examples

```
feat(connector): add PostgreSQL connector implementation

Implement PostgreSQL database connector with connection pooling
support using HikariCP. Includes schema discovery and query
pushdown capabilities.

Closes #123
```

```
fix(optimizer): correct predicate pushdown for nested queries

Fixed an issue where predicates were not correctly pushed down
through nested subqueries, causing performance degradation.

Fixes #456
```

```
docs(readme): update quick start guide

Added more detailed instructions for local development setup.
```

### Commit Best Practices

1. **Keep commits atomic**: One logical change per commit
2. **Write clear messages**: Explain what and why, not how
3. **Reference issues**: Include issue numbers when applicable
4. **Sign your work**: Use `git commit -s` for DCO sign-off

## Pull Request Process

### Before Submitting

1. **Sync with upstream**:
   ```bash
   git fetch upstream
   git rebase upstream/main
   ```

2. **Run all checks**:
   ```bash
   ./mvnw clean verify -Pcheck
   ```

3. **Ensure tests pass**:
   ```bash
   ./mvnw test
   ```

4. **Update documentation**: If applicable, update README.md or other docs

### PR Requirements

- [ ] Code compiles without warnings
- [ ] All tests pass
- [ ] Code coverage maintained or improved
- [ ] Checkstyle passes
- [ ] Spotless formatting applied
- [ ] Documentation updated (if applicable)
- [ ] Commit messages follow guidelines
- [ ] PR description explains the change

### PR Description Template

```markdown
## Summary
Brief description of the changes.

## Changes
- List of specific changes made
- Another change

## Testing
How was this tested?

## Related Issues
Fixes #123
```

### Review Process

1. At least one approval required from a maintainer
2. All CI checks must pass
3. No merge conflicts
4. Address all review comments

### After Merge

- Delete your feature branch
- Sync your fork with upstream

## Testing Guidelines

### Unit Tests

- Use JUnit 5
- Use AssertJ for assertions
- Use Mockito for mocking
- Name tests descriptively: `shouldReturnEmptyListWhenNoResults()`

```java
@Test
void shouldParseSimpleSelectStatement() {
    // Given
    String sql = "SELECT * FROM users";

    // When
    SqlNode result = parser.parse(sql);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getKind()).isEqualTo(SqlKind.SELECT);
}
```

### Integration Tests

- Use Testcontainers for database tests
- Place integration tests in `src/test/java` with `*IT.java` suffix
- Use `@Tag("integration")` annotation

```java
@Test
@Tag("integration")
void shouldExecuteQueryAcrossMultipleDatabases() {
    try (MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");
         PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")) {

        mysql.start();
        postgres.start();

        // Test implementation
    }
}
```

### Test Coverage

- Maintain at least 80% code coverage
- Use JaCoCo for coverage reporting:
  ```bash
  ./mvnw jacoco:report
  ```

## Questions?

If you have questions, feel free to:
- Open an issue for discussion
- Reach out to maintainers

Thank you for contributing to IntelliSql!
