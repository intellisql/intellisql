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

package com.intellisql.parser;

import org.apache.calcite.plan.Convention;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelTraitDef;
import org.apache.calcite.rel.RelNode;

/**
 * IntelliSql convention definitions for Calcite relational expressions. Defines the convention used
 * for IntelliSql query processing.
 */
public final class IntelliSqlConventions {

    /** IntelliSql convention instance. */
    @SuppressWarnings("unchecked")
    public static final Convention INTELLISQL = new Convention.Impl("INTELLISQL", RelNode.class);

    private IntelliSqlConventions() {
    }

    /** Trait definition for IntelliSql convention. */
    public static class IntelliSqlTraitDef extends RelTraitDef<Convention> {

        public static final IntelliSqlTraitDef INSTANCE = new IntelliSqlTraitDef();

        @Override
        public Class<Convention> getTraitClass() {
            return Convention.class;
        }

        @Override
        public String getSimpleName() {
            return "INTELLISQL";
        }

        @Override
        public Convention getDefault() {
            return INTELLISQL;
        }

        @Override
        public boolean canConvert(final RelOptPlanner planner, final Convention fromTrait, final Convention toTrait) {
            return true;
        }

        @Override
        public RelNode convert(
                               final RelOptPlanner planner,
                               final RelNode rel,
                               final Convention toConvention,
                               final boolean allowInfiniteCostConverters) {
            return null;
        }
    }
}
