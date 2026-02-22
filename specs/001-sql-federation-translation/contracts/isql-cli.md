# Contract: isql Command Line Interface

**Version**: 1.0.0
**Status**: Draft
**Format**: CLI Arguments & Interactive Commands

## Overview

`isql` is the official command-line interface for IntelliSql. It supports two modes:
1. **Interactive Mode (REPL)**: Connect to a server and execute SQL queries interactively.
2. **Batch Mode**: Execute SQL scripts from a file or command line argument.

## Command Line Arguments

```bash
isql [options] [command]
```

### Global Options

| Option | Description | Default |
|--------|-------------|---------|
| `-h`, `--help` | Show this help message and exit. | - |
| `-v`, `--version` | Print version information and exit. | - |
| `-u`, `--user <name>` | Database user for connection. | (Prompt if needed) |
| `-p`, `--password <pass>` | Database password. | (Prompt if needed) |
| `-H`, `--host <host>` | Server host. | localhost |
| `-P`, `--port <port>` | Server port. | 8765 |
| `-D`, `--database <db>` | Initial database to connect to. | default |

### Commands

#### 1. Interactive Shell (Default)

Starts the interactive shell.

```bash
isql [options]
```

**Example**:
```bash
isql -u admin -p secret -H localhost -P 8765
```

#### 2. Execute Script (Batch)

Executes a SQL script file.

```bash
isql -f <file> [options]
```

**Options**:
- `-f`, `--file <file>`: Path to the SQL script file.

**Example**:
```bash
isql -f query.sql -u admin
```

#### 3. Translate SQL

Translates a SQL statement from one dialect to another without executing it.

```bash
isql translate --from <dialect> --to <dialect> "<sql>"
```

**Options**:
- `--from <dialect>`: Source dialect (mysql, postgresql, oracle, sqlserver, hive).
- `--to <dialect>`: Target dialect.
- `<sql>`: The SQL string to translate.

**Example**:
```bash
isql translate --from mysql --to oracle "SELECT * FROM users LIMIT 10"
```

## Interactive Mode Commands (Meta-commands)

Inside the interactive shell, lines starting with `` are treated as meta-commands.

| Command | Arguments | Description |
|---------|-----------|-------------|
| `\connect`, `\c` | `[url] [user] [password]` | Connect to a database server. |
| `\disconnect` | - | Disconnect from the current server. |
| `\databases`, `\l` | - | List available databases (schemas). |
| `	ables`, `\dt` | `[pattern]` | List tables in the current database. |
| `\describe`, `\d` | `<table>` | Describe table schema (columns). |
| `\history` | - | Show command history. |
| `\help`, `\?` | - | Show help for interactive commands. |
| `\quit`, `\q`, `exit` | - | Exit the interactive shell. |

## Key Behaviors

### 1. Syntax Highlighting
- **Keywords**: Blue/Bold (e.g., SELECT, FROM, WHERE)
- **Strings**: Yellow (e.g., 'value')
- **Comments**: Grey (e.g., -- comment)
- **Numbers**: Magenta

### 2. Auto-completion
- Triggered by `TAB`.
- **Context-aware**:
    - After `FROM` or `JOIN`: Suggests table names.
    - After `SELECT`, `WHERE`, `GROUP BY`: Suggests column names (if table is known).
    - Start of line: Suggests keywords and meta-commands.

### 3. Paging
- For large result sets (> screen height), output is piped to a built-in pager.
- **Keys**:
    - `SPACE`, `f`, `PGDN`: Next page.
    - `b`, `PGUP`: Previous page.
    - `q`: Quit paging and return to prompt.
    - `j`, `DOWN`: Scroll down one line.
    - `k`, `UP`: Scroll up one line.

### 4. Signal Handling
- `Ctrl+C`:
    - If a query is running: Cancels the query (sends cancel signal to server) but keeps the shell open.
    - If at prompt with input: Clears the current line.
    - If at prompt without input: Does nothing (or hints to use `\q` to exit).
- `Ctrl+D`: Exits the shell (EOF).
