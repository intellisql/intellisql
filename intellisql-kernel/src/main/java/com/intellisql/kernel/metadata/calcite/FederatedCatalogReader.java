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

package com.intellisql.kernel.metadata.calcite;

import org.apache.calcite.config.CalciteConnectionConfig;
import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.prepare.CalciteCatalogReader;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.sql.validate.SqlNameMatchers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Federated catalog reader for Calcite.
 * Extends CalciteCatalogReader to support table lookup without schema prefix.
 * Reference: ShardingSphere SQLFederationCatalogReader.
 */
public final class FederatedCatalogReader extends CalciteCatalogReader {

    /**
     * Creates a new FederatedCatalogReader.
     *
     * @param rootSchema the root Calcite schema
     * @param schemaPath the schema path
     * @param typeFactory the type factory
     * @param config the connection config
     */
    public FederatedCatalogReader(
                                  final CalciteSchema rootSchema,
                                  final List<String> schemaPath,
                                  final RelDataTypeFactory typeFactory,
                                  final CalciteConnectionConfig config) {
        super(rootSchema,
                SqlNameMatchers.withCaseSensitive(config.caseSensitive()),
                Arrays.asList(Collections.emptyList(), schemaPath),
                typeFactory,
                config);
    }
}
