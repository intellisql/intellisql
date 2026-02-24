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

package com.intellisql.client.console;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CompleterFactoryTest {

    @Test
    void testCreate() {
        MetaDataLoader loader = mock(MetaDataLoader.class);
        Set<String> tables = new HashSet<>();
        tables.add("my_table");
        when(loader.getTables()).thenReturn(tables);
        when(loader.getColumns()).thenReturn(new HashSet<>());
        when(loader.getSchemas()).thenReturn(new HashSet<>());
        Completer completer = CompleterFactory.create(loader);
        LineReader reader = mock(LineReader.class);
        ParsedLine line = mock(ParsedLine.class);
        List<Candidate> candidates = new ArrayList<>();
        // Test keyword completion
        when(line.word()).thenReturn("SEL");
        completer.complete(reader, line, candidates);
        assertTrue(candidates.stream().anyMatch(c -> c.value().equals("SELECT")));
        // Test table completion
        when(line.word()).thenReturn("my_");
        completer.complete(reader, line, candidates);
        assertTrue(candidates.stream().anyMatch(c -> c.value().equals("my_table")));
    }
}
