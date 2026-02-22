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

package com.intellisql.client.command;

import com.intellisql.client.ClientException;
import com.intellisql.client.IntelliSqlClient;
import com.intellisql.client.ResultFormatter;
import com.intellisql.common.dialect.SqlDialect;

import com.intellisql.translator.SqlTranslator;
import com.intellisql.translator.Translation;
import com.intellisql.translator.TranslationMode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Command to translate SQL between dialects. */
@Slf4j
@Getter
@RequiredArgsConstructor
public class TranslateCommand implements Command {

    private final IntelliSqlClient client;

    private final String sql;

    private final SqlDialect sourceDialect;

    private final SqlDialect targetDialect;

    @Override
    public void execute() throws ClientException {
        log.debug("Translating SQL: {}", sql);
        SqlDialect source = sourceDialect != null ? sourceDialect : SqlDialect.MYSQL;
        SqlDialect target = targetDialect != null ? targetDialect : SqlDialect.POSTGRESQL;
        SqlTranslator translator = new SqlTranslator();
        Translation request = Translation.create(sql, source, target, TranslationMode.OFFLINE);
        Translation result = translator.translate(request);
        ResultFormatter formatter = client.getResultFormatter();
        if (result.isSuccessful()) {
            System.out.println(formatter.formatTranslation(sql, result.getTargetSql()));
            if (result.hasUnsupportedFeatures()) {
                System.out.println("Unsupported features: " + result.getUnsupportedFeatures());
            }
        } else {
            throw new ClientException("Translation failed: " + result.getError().getMessage());
        }
    }

    @Override
    public String getDescription() {
        return "Translate SQL from " + sourceDialect + " to " + targetDialect;
    }
}
