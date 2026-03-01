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

package com.intellisql.client.console;

import org.jline.reader.Completer;
import org.jline.reader.impl.completer.AggregateCompleter;
import org.jline.reader.impl.completer.StringsCompleter;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating completers for SQL and isql commands.
 */
public class CompleterFactory {

    private static final String[] SQL_KEYWORDS = {
            "SELECT", "INSERT", "UPDATE", "DELETE", "FROM", "WHERE", "AND", "OR",
            "ORDER", "BY", "GROUP", "LIMIT", "OFFSET", "JOIN", "LEFT", "RIGHT",
            "INNER", "OUTER", "ON", "AS", "IN", "IS", "NULL", "NOT", "CREATE",
            "DROP", "TABLE", "DATABASE", "INDEX", "VIEW", "ALTER", "ADD", "COLUMN",
            "SET", "VALUES", "INTO", "UNION", "ALL", "DISTINCT", "HAVING", "CASE",
            "WHEN", "THEN", "ELSE", "END", "EXISTS", "LIKE", "BETWEEN", "DESC", "ASC"
    };

    private static final String[] ISQL_COMMANDS = {
            "\\connect", "\\quit", "\\help", "\\translate", "exit"
    };

    /**
     * Creates an aggregate completer with SQL keywords, isql commands, and database metadata.
     *
     * @param loader the metadata loader for dynamic completions
     * @return the aggregate completer
     */
    public static Completer create(final MetaDataLoader loader) {
        List<Completer> completers = new ArrayList<>();
        // Keywords
        completers.add(new StringsCompleter(SQL_KEYWORDS));
        // Commands
        completers.add(new StringsCompleter(ISQL_COMMANDS));
        // Dynamic metadata
        completers.add((reader, line, candidates) -> {
            new StringsCompleter(loader.getTables()).complete(reader, line, candidates);
            new StringsCompleter(loader.getColumns()).complete(reader, line, candidates);
            new StringsCompleter(loader.getSchemas()).complete(reader, line, candidates);
        });
        return new AggregateCompleter(completers);
    }
}
