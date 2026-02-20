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

package com.intellisql.parser.ast;

import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlOperator;
import org.apache.calcite.sql.SqlSpecialOperator;
import org.apache.calcite.sql.SqlWriter;
import org.apache.calcite.sql.parser.SqlParserPos;
import java.util.Collections;
import java.util.List;

/**
 * A <code>SqlUseSchema</code> represents a USE statement.
 * This statement is used to set the default schema/database for the session.
 * Syntax: USE schema_name
 */
public class SqlUseSchema extends SqlCall {

    public static final SqlSpecialOperator OPERATOR =
            new SqlSpecialOperator("USE_SCHEMA", SqlKind.OTHER) {

                @Override
                public SqlCall createCall(final SqlLiteral functionQualifier, final SqlParserPos pos,
                                          final SqlNode... operands) {
                    return new SqlUseSchema(pos, (SqlIdentifier) operands[0]);
                }
            };

    private final SqlIdentifier schema;

    /**
     * Creates a SqlUseSchema.
     *
     * @param pos position
     * @param schema schema/database name
     */
    public SqlUseSchema(final SqlParserPos pos, final SqlIdentifier schema) {
        super(pos);
        this.schema = schema;
    }

    @Override
    public SqlOperator getOperator() {
        return OPERATOR;
    }

    @Override
    public List<SqlNode> getOperandList() {
        return Collections.singletonList(schema);
    }

    @Override
    public void unparse(final SqlWriter writer, final int leftPrec, final int rightPrec) {
        writer.keyword("USE");
        schema.unparse(writer, leftPrec, rightPrec);
    }

    /**
     * Returns the schema name.
     *
     * @return schema name
     */
    public SqlIdentifier getSchema() {
        return schema;
    }
}
