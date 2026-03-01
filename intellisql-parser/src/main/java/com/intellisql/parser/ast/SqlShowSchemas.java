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

package com.intellisql.parser.ast;

import org.apache.calcite.sql.SqlCall;
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
 * A <code>SqlShowSchemas</code> represents a SHOW SCHEMAS or SHOW DATABASES statement.
 * This statement is used to list schemas/databases.
 * Syntax: SHOW {SCHEMAS | DATABASES} [LIKE pattern]
 */
public class SqlShowSchemas extends SqlCall {

    public static final SqlSpecialOperator OPERATOR =
            new SqlSpecialOperator("SHOW_SCHEMAS", SqlKind.OTHER) {

                @Override
                public SqlCall createCall(final SqlLiteral functionQualifier, final SqlParserPos pos,
                                          final SqlNode... operands) {
                    return new SqlShowSchemas(pos, operands[0]);
                }
            };

    private final SqlNode likePattern;

    /**
     * Creates a SqlShowSchemas.
     *
     * @param pos position
     * @param likePattern LIKE pattern, may be null
     */
    public SqlShowSchemas(final SqlParserPos pos, final SqlNode likePattern) {
        super(pos);
        this.likePattern = likePattern;
    }

    @Override
    public SqlOperator getOperator() {
        return OPERATOR;
    }

    @Override
    public List<SqlNode> getOperandList() {
        return ImmutableNullableList.of(likePattern);
    }

    @Override
    public void unparse(final SqlWriter writer, final int leftPrec, final int rightPrec) {
        writer.keyword("SHOW");
        writer.keyword("SCHEMAS");
        if (likePattern != null) {
            writer.keyword("LIKE");
            likePattern.unparse(writer, leftPrec, rightPrec);
        }
    }

    /**
     * Returns the LIKE pattern.
     *
     * @return LIKE pattern, may be null
     */
    public SqlNode getLikePattern() {
        return likePattern;
    }
}
