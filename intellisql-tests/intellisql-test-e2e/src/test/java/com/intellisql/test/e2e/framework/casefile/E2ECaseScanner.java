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

package com.intellisql.test.e2e.framework.casefile;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/** Scanner for JDBC E2E SQL case resources. */
public final class E2ECaseScanner {

    private final E2ECaseParser parser;

    /**
     * Creates a case scanner.
     *
     * @param parser the SQL case parser
     */
    public E2ECaseScanner(final E2ECaseParser parser) {
        this.parser = parser;
    }

    /**
     * Scans SQL case resources below the specified classpath root.
     *
     * @param caseRoot the classpath case root
     * @return parsed test cases
     * @throws IllegalStateException if the case root cannot be scanned
     */
    public List<E2ETestCase> scan(final String caseRoot) {
        Path root = resolveRoot(caseRoot);
        List<Path> files = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".sql")).forEach(files::add);
        } catch (final IOException ex) {
            throw new IllegalStateException("Failed to scan case root: " + caseRoot, ex);
        }
        files.sort(Comparator.comparing(Path::toString));
        List<E2ETestCase> result = new ArrayList<>(files.size());
        for (Path each : files) {
            result.add(parser.parse(each));
        }
        return result;
    }

    private Path resolveRoot(final String caseRoot) {
        URL resource = getClass().getClassLoader().getResource(caseRoot);
        if (resource == null) {
            throw new IllegalArgumentException("Case root resource not found: " + caseRoot);
        }
        try {
            return Paths.get(resource.toURI());
        } catch (final URISyntaxException ex) {
            throw new IllegalArgumentException("Invalid case root URI: " + caseRoot, ex);
        }
    }
}
