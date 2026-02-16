# IntelliSql JDBC Driver Distribution

This is the standalone JDBC driver distribution for IntelliSql.

## Files

- `intellisql-jdbc-{version}.jar`: JDBC driver with all dependencies bundled

## Requirements

- Java 8 or higher

## Usage

### Adding to Your Project

#### Maven

Add the JAR file to your local Maven repository or include it directly:

```xml
<dependency>
    <groupId>org.intellisql</groupId>
    <artifactId>intellisql-jdbc</artifactId>
    <version>{version}</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/lib/intellisql-jdbc-{version}.jar</systemPath>
</dependency>
```

#### Gradle

```groovy
implementation files('lib/intellisql-jdbc-{version}.jar')
```

#### Manual

Copy the JAR file to your application's classpath.

### Connecting to IntelliSql Server

```java
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class IntelliSqlEmample {
    public static void main(String[] args) throws Exception {
        // Native IntelliSql JDBC connection
        String url = "jdbc:intellisql://localhost:8765";
        try (Connection conn = DriverManager.getConnection(url)) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM my_table");
            while (rs.next()) {
                System.out.println(rs.getString(1));
            }
        }
    }
}
```

### Connecting via Avatica HTTP

```java
// Avatica HTTP connection
String url = "jdbc:avatica:remote:url=http://localhost:8766";
try (Connection conn = DriverManager.getConnection(url)) {
    // ... execute queries
}
```

## Connection URL Format

### Native IntelliSql JDBC

```
jdbc:intellisql://[host]:[port]/[database]?[property1]=[value1]&[property2]=[value2]
```

Parameters:
- `host`: Server hostname (default: localhost)
- `port`: Server port (default: 8765)
- `database`: Optional database/catalog name

### Avatica HTTP

```
jdbc:avatica:remote:url=http://[host]:[port]
```

Parameters:
- `host`: Server hostname (default: localhost)
- `port`: Avatica HTTP port (default: 8766)

## Connection Properties

| Property | Description | Default |
|----------|-------------|---------|
| `user` | Username for authentication | (none) |
| `password` | Password for authentication | (none) |
| `connectTimeout` | Connection timeout in milliseconds | 30000 |
| `socketTimeout` | Socket read timeout in milliseconds | 0 (infinite) |
| `fetchSize` | Default fetch size for queries | 100 |

## Supported SQL Features

The IntelliSql JDBC driver supports:
- Standard SQL queries (SELECT, INSERT, UPDATE, DELETE)
- Prepared statements
- Batch operations
- Transaction management
- Metadata queries (DatabaseMetaData)
- Multiple data source federation

## Example Code

```java
import java.sql.*;

public class IntelliSqlDemo {
    public static void main(String[] args) {
        String url = "jdbc:intellisql://localhost:8765";

        try (Connection conn = DriverManager.getConnection(url)) {
            // Query metadata
            DatabaseMetaData meta = conn.getMetaData();
            System.out.println("Product: " + meta.getDatabaseProductName());
            System.out.println("Version: " + meta.getDatabaseProductVersion());

            // Execute a query
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT 1 AS result")) {
                while (rs.next()) {
                    System.out.println("Result: " + rs.getInt("result"));
                }
            }

            // Prepared statement example
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT * FROM users WHERE id = ?")) {
                pstmt.setInt(1, 100);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        System.out.println("User: " + rs.getString("name"));
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
```

## Support

For more information, visit: https://github.com/intellisql/intellisql
