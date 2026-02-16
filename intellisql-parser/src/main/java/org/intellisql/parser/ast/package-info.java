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

/**
 * Custom AST (Abstract Syntax Tree) node classes for IntelliSql parser extensions.
 * These nodes extend Apache Calcite's SqlNode hierarchy to support additional
 * SQL syntax not covered by the standard Calcite parser.
 *
 * <p>Supported custom statements:</p>
 * <ul>
 *   <li>{@link org.intellisql.parser.ast.SqlShowTables} - SHOW TABLES statement</li>
 *   <li>{@link org.intellisql.parser.ast.SqlShowSchemas} - SHOW SCHEMAS/DATABASES statement</li>
 *   <li>{@link org.intellisql.parser.ast.SqlUseSchema} - USE statement</li>
 * </ul>
 *
 * @see org.apache.calcite.sql.SqlNode
 * @see org.apache.calcite.sql.SqlCall
 */
package org.intellisql.parser.ast;
