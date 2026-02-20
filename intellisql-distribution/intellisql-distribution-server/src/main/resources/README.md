# IntelliSql Server Distribution

This is the server distribution package for IntelliSql.

## Directory Structure

```
intellisql-server-{version}/
  bin/                    # Startup and shutdown scripts
    start.sh              # Unix/Linux startup script
    stop.sh               # Unix/Linux stop script
    start.bat             # Windows startup script
    stop.bat              # Windows stop script
    setenv.sh             # Unix/Linux environment settings
    setenv.bat            # Windows environment settings
  conf/                   # Configuration files
    server.yml            # Server configuration
    logback.xml           # Logging configuration
  lib/                    # All JAR dependencies
  logs/                   # Log files (created on first run)
  LICENSE                 # Apache License 2.0
  NOTICE                  # Attribution notices
  README.md               # This file
```

## Requirements

- Java 8 or higher
- Operating System: Linux, macOS, or Windows

## Quick Start

### Unix/Linux/macOS

1. Configure the server by editing `conf/server.yml`

2. Start the server:
   ```bash
   ./bin/start.sh
   ```

3. Stop the server:
   ```bash
   ./bin/stop.sh
   ```

### Windows

1. Configure the server by editing `conf\server.yml`

2. Start the server:
   ```cmd
   bin\start.bat
   ```

3. Stop the server:
   ```cmd
   bin\stop.bat
   ```

## Configuration

### Server Configuration (conf/server.yml)

The main configuration file is `server.yml`. Key settings include:

- `server.host`: Server bind address (default: 0.0.0.0)
- `server.port`: Server port (default: 8765)
- `avatica.port`: Avatica JDBC port (default: 8766)
- `datasources`: Data source configurations

### Environment Variables

You can customize the server behavior using environment variables:

- `JAVA_HOME`: Path to Java installation
- `JAVA_OPTS`: JVM options (e.g., memory settings)
- `INTELLISQL_CONF`: Path to custom configuration file
- `LOG_CONFIG`: Path to custom logging configuration

### Logging Configuration (conf/logback.xml)

The logging configuration uses Logback. You can modify:
- Log level (INFO, DEBUG, etc.)
- Log file location and rotation
- Log format

## Connecting to the Server

Once the server is running, you can connect using the JDBC driver:

```java
String url = "jdbc:intellisql://localhost:8765";
Connection conn = DriverManager.getConnection(url);
```

Or connect to the Avatica HTTP server:

```java
String url = "jdbc:avatica:remote:url=http://localhost:8766";
Connection conn = DriverManager.getConnection(url);
```

## Monitoring

### JMX

Enable JMX monitoring by uncommenting the JMX options in `setenv.sh` or `setenv.bat`:

```bash
JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=9010"
```

### Logs

Server logs are located in the `logs/` directory:
- `intellisql.log`: Main server log
- `startup.log`: Startup output

## Support

For more information, visit: https://github.com/intellisql/intellisql
