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

package com.intellisql.test.e2e.framework.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Reads classpath resources for JDBC E2E tests. */
public final class ResourceReader {

    private static final int BUFFER_SIZE = 4096;

    /**
     * Reads a UTF-8 classpath resource.
     *
     * @param resourcePath the classpath resource path
     * @return the resource content
     * @throws IllegalStateException if the resource cannot be read
     */
    public String read(final String resourcePath) {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Resource not found: " + resourcePath);
            }
            return readStream(inputStream);
        } catch (final IOException ex) {
            throw new IllegalStateException("Failed to read resource: " + resourcePath, ex);
        }
    }

    /**
     * Checks whether a classpath resource exists.
     *
     * @param resourcePath the classpath resource path
     * @return true when the resource exists
     */
    public boolean exists(final String resourcePath) {
        return getClass().getClassLoader().getResource(resourcePath) != null;
    }

    private String readStream(final InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_SIZE];
        int length;
        while ((length = inputStream.read(buffer)) >= 0) {
            outputStream.write(buffer, 0, length);
        }
        return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
    }
}
