# IntelliSql

IntelliSql - SQL Federation and Translation Platform

English | [简体中文](README_CN.md)

## Overview

IntelliSql is a distributed SQL federation and translation platform that enables unified querying across multiple heterogeneous data sources. Built on Apache Calcite, it provides:

- **SQL Federation**: Query multiple databases (MySQL, PostgreSQL, Elasticsearch) through a single SQL interface
- **SQL Translation**: Translate SQL dialects between different database systems
- **JDBC Protocol**: Standard JDBC interface for compatibility with existing tools
- **Query Optimization**: Cost-based query optimization with predicate pushdown

## Features

- Multi-database federation support
- SQL dialect translation (MySQL, PostgreSQL, Elasticsearch)
- Standard JDBC protocol via Avatica
- Query optimization and pushdown
- Connection pooling with HikariCP
- JSON structured logging
- Health monitoring and metrics

## Quick Start

### Prerequisites

- Java 8 or higher
- Maven 3.6+
- Docker (optional, for testing)

### Build

```bash
# Clone the repository
git clone https://github.com/intellisql/intellisql.git
cd intellisql

# Build the project
./mvnw clean install -DskipTests

# Build with tests
./mvnw clean install
```

### Configuration

1. After extracting the distribution, the configuration files are in `conf/` directory:

```bash
cd intellisql-server-1.0.0-SNAPSHOT
ls conf/
# model.yaml  logback.xml
```

2. Edit `conf/model.yaml` to add your data sources:

```yaml
dataSources:
  - name: mysql_source
    type: mysql
    host: localhost
    port: 3306
    database: source_db
    username: root
    password: ${MYSQL_PASSWORD}
```

### Run

```bash
# Build and extract distribution
./mvnw clean package -DskipTests
cd intellisql-distribution/intellisql-distribution-server/target
unzip intellisql-server-1.0.0-SNAPSHOT.zip
cd intellisql-server-1.0.0-SNAPSHOT

# Start the server
./bin/start.sh

# Start with remote debugging
./bin/start.sh --debug

# Stop the server
./bin/stop.sh
```

### CLI Client (isql)

#### Connection Options

| Option | Description | Default |
|--------|-------------|---------|
| `-h, --host` | Server host | localhost |
| `-P, --port` | Server port | 8765 |
| `-u, --user` | Username | root |
| `-p, --password` | Password | (none) |
| `-d, --database` | Database name | intellisql |

#### Usage Examples

```bash
# Connect to local server (interactive mode)
./bin/isql

# Connect to remote server
./bin/isql -h db.example.com -P 8765 -u admin -p secret

# Connect with specific database
./bin/isql -d mydb

# Execute a single query and exit
./bin/isql -e "SELECT * FROM users LIMIT 10"

# Execute a SQL script file
./bin/isql -f /path/to/queries.sql
```

#### Interactive Mode Commands

Once connected, you can use these commands:

| Command | Description |
|---------|-------------|
| `\h` or `\help` | Show help |
| `\q` or `\quit` | Exit the client |
| `\c` or `\clear` | Clear input buffer |
| `\t <sql>` | Translate SQL between dialects |
| `\s <file>` | Execute script file |
| `\d` | List data sources |

```bash
# Interactive session example
isql> SELECT * FROM users WHERE id = 1;
isql> \t SELECT DATE_FORMAT(created_at, '%Y-%m') FROM users;
isql> \s /path/to/batch.sql
isql> \q
```

## Project Structure

```
intellisql/
├── intellisql-parser/      # SQL parsing module
├── intellisql-optimizer/   # Query optimization module
├── intellisql-executor/    # Query execution module
├── intellisql-connector/   # Database connectors
├── intellisql-kernel/      # Core kernel module
├── intellisql-jdbc/        # JDBC driver implementation
├── intellisql-server/      # Server implementation
│   └── src/main/resources/
│       └── conf/           # Configuration files
│           ├── model.yaml  # Main configuration
│           └── logback.xml # Logging configuration
├── intellisql-client/      # CLI client
├── intellisql-distribution/
│   └── intellisql-distribution-server/
│       └── bin/            # Startup scripts
│           ├── start.sh    # Server start script
│           ├── stop.sh     # Server stop script
│           └── isql        # CLI client script
└── intellisql-test/        # Integration tests
```

## Module Descriptions

### intellisql-parser

SQL parsing module based on Apache Calcite. Parses SQL statements into abstract syntax trees (AST).

### intellisql-optimizer

Query optimization module. Implements cost-based optimization, predicate pushdown, and join optimization.

### intellisql-executor

Query execution engine. Executes optimized query plans across multiple data sources.

### intellisql-connector

Database connector implementations. Provides unified interface for MySQL, PostgreSQL, and Elasticsearch.

### intellisql-kernel

Core kernel module containing shared utilities, configuration management, and common interfaces.

### intellisql-jdbc

JDBC driver implementation using Apache Avatica. Enables standard JDBC connectivity.

### intellisql-server

Server module that exposes JDBC protocol and handles client connections.

### intellisql-client

Command-line interface client for interactive SQL queries.

### intellisql-distribution

Assembly module that packages all components into a distributable archive.

### intellisql-test

Integration tests using Testcontainers for testing against real database instances.

## Development

### Code Style

This project uses:

- **Spotless** for code formatting (Palantir Java Format)
- **Checkstyle** for code quality checks

```bash
# Format code
./mvnw spotless:apply

# Check code style
./mvnw checkstyle:check

# Run all checks
./mvnw clean verify -Pcheck
```

### Testing

```bash
# Run unit tests
./mvnw test

# Run integration tests
./mvnw verify -DskipUnitTests

# Run all tests
./mvnw verify
```

### IDE Setup

Import the project as a Maven project in your IDE (IntelliJ IDEA recommended).

Make sure to:
1. Enable annotation processing for Lombok
2. Install Lombok plugin
3. Set Java 8 as project SDK

## Configuration Reference

### Server Configuration

| Property | Description | Default |
|----------|-------------|---------|
| server.host | Server bind address | 0.0.0.0 |
| server.port | HTTP port | 8765 |
| server.avaticaPort | JDBC/Avatica port | 8766 |

### Data Source Configuration

| Property | Description | Required |
|----------|-------------|----------|
| name | Data source name | Yes |
| type | Database type (mysql, postgresql, elasticsearch) | Yes |
| host | Database host | Yes |
| port | Database port | Yes |
| database | Database name | Yes |
| username | Connection username | Yes |
| password | Connection password | No |

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [Apache Calcite](https://calcite.apache.org/) - Dynamic data management framework
- [Apache Avatica](https://calcite.apache.org/avatica/) - JDBC driver framework
- [ShardingSphere](https://shardingsphere.apache.org/) - Inspiration for architecture and code style
