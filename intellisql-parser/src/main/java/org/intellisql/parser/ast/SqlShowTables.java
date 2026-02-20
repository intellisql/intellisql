/*
 * Licensed to the IntelliSQL Project under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The IntelliSQL Project licenses this file to You under the Apache License, Version 2.0
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

package org.intellisql.parser.ast;

import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlOperator;
import org.apache.calcite.sql.SqlSpecialOperator;
import org.apache.calcite.sql.SqlWriter;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.util.ImmutableNullableList;
import java.util.List;

/**
 * A <code>SqlShowTables</code> represents a SHOW TABLES statement.
 * This statement is used to list tables in a database/schema.
 * Syntax: SHOW TABLES [FROM|IN schema] [LIKE pattern] [WHERE condition]
 */
public class SqlShowTables extends SqlCall {

    public static final SqlSpecialOperator OPERATOR =
            new SqlSpecialOperator("SHOW_TABLES", SqlKind.OTHER) {

                @Override
                public SqlCall createCall(final SqlLiteral functionQualifier, final SqlParserPos pos,
                                          final SqlNode... operands) {
                    return new SqlShowTables(pos, (SqlIdentifier) operands[0], operands[1], operands[2]);
                }
            };

    private final SqlIdentifier db;

    private final SqlNode likePattern;

    private final SqlNode where;

    /**
     * Creates a SqlShowTables.
     *
     * @param pos position
     * @param db database/schema name, may be null
     * @param likePattern LIKE pattern, may be null
     * @param where WHERE condition, may be null
     */
    public SqlShowTables(final SqlParserPos pos, final SqlIdentifier db,
                         final SqlNode likePattern, final SqlNode where) {
        super(pos);
        this.db = db;
        this.likePattern = likePattern;
        this.where = where;
    }

    @Override
    public SqlOperator getOperator() {
        return OPERATOR;
    }

    @Override
    public List<SqlNode> getOperandList() {
        return ImmutableNullableList.of(db, likePattern, where);
    }

    @Override
    public void unparse(final SqlWriter writer, final int leftPrec, final int rightPrec) {
        writer.keyword("SHOW");
        writer.keyword("TABLES");
        if (db != null) {
            writer.keyword("FROM");
            db.unparse(writer, leftPrec, rightPrec);
        }
        if (likePattern != null) {
            writer.keyword("LIKE");
            likePattern.unparse(writer, leftPrec, rightPrec);
        }
        if (where != null) {
            writer.keyword("WHERE");
            where.unparse(writer, leftPrec, rightPrec);
        }
    }

    /**
     * Returns the database/schema name.
     *
     * @return database name, may be null
     */
    public SqlIdentifier getDb() {
        return db;
    }

    /**
     * Returns the LIKE pattern.
     *
     * @return LIKE pattern, may be null
     */
    public SqlNode getLikePattern() {
        return likePattern;
    }

    /**
     * Returns the WHERE condition.
     *
     * @return WHERE condition, may be null
     */
    public SqlNode getWhere() {
        return where;
    }
}
