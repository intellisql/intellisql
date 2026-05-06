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

package com.intellisql.test.e2e.framework.environment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import com.intellisql.test.e2e.framework.io.ResourceReader;

/** Renders IntelliSQL model configuration templates for JDBC E2E tests. */
public final class ModelConfigRenderer {

    private final ResourceReader resourceReader = new ResourceReader();

    /**
     * Renders a model configuration template.
     *
     * @param model the model name
     * @param variables the template variables
     * @param targetDirectory the target directory
     * @return rendered model configuration path
     * @throws IllegalStateException if the model configuration cannot be rendered
     */
    public Path render(final String model, final Map<String, String> variables, final Path targetDirectory) {
        String content = readTemplate(model);
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            content = content.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        if (content.contains("${")) {
            throw new IllegalStateException("Unresolved model template variable in model: " + model);
        }
        try {
            Files.createDirectories(targetDirectory);
            Path result = targetDirectory.resolve("model.yaml");
            Files.write(result, content.getBytes(StandardCharsets.UTF_8));
            return result;
        } catch (final IOException ex) {
            throw new IllegalStateException("Failed to render model config: " + model, ex);
        }
    }

    private String readTemplate(final String model) {
        String resourcePath = "e2e/models/" + model + "/model.yaml";
        return resourceReader.read(resourcePath);
    }
}
