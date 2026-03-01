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

package com.intellisql.client.console;

import org.jline.terminal.Terminal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Tests for TerminalPrinter.
 * Uses a dynamic proxy to create a minimal Terminal implementation instead of Mockito
 * to ensure compatibility with JDK 21+ where mocking certain interfaces is restricted.
 */
class TerminalPrinterTest {

    @Test
    void testPrint() {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        StubTerminalHandler handler = new StubTerminalHandler(printWriter);
        Terminal terminal = createStubTerminal(handler);
        TerminalPrinter printer = new TerminalPrinter(terminal);
        printer.print("Hello");
        Assertions.assertEquals("Hello", stringWriter.toString());
        Assertions.assertTrue(handler.isFlushed(), "Terminal should be flushed");
    }

    @Test
    void testPrintln() {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        StubTerminalHandler handler = new StubTerminalHandler(printWriter);
        Terminal terminal = createStubTerminal(handler);
        TerminalPrinter printer = new TerminalPrinter(terminal);
        printer.println("Hello");
        Assertions.assertEquals("Hello" + System.lineSeparator(), stringWriter.toString());
        Assertions.assertTrue(handler.isFlushed(), "Terminal should be flushed");
    }

    @Test
    void testMultiplePrints() {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        StubTerminalHandler handler = new StubTerminalHandler(printWriter);
        Terminal terminal = createStubTerminal(handler);
        TerminalPrinter printer = new TerminalPrinter(terminal);
        printer.print("Hello");
        printer.print(" ");
        printer.print("World");
        Assertions.assertEquals("Hello World", stringWriter.toString());
    }

    @Test
    void testMultiplePrintlns() {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        StubTerminalHandler handler = new StubTerminalHandler(printWriter);
        Terminal terminal = createStubTerminal(handler);
        TerminalPrinter printer = new TerminalPrinter(terminal);
        String lineSeparator = System.lineSeparator();
        printer.println("Line1");
        printer.println("Line2");
        Assertions.assertEquals("Line1" + lineSeparator + "Line2" + lineSeparator, stringWriter.toString());
    }

    /**
     * Creates a stub Terminal proxy.
     *
     * @param handler the invocation handler
     * @return a Terminal proxy instance
     */
    private Terminal createStubTerminal(final StubTerminalHandler handler) {
        return (Terminal) Proxy.newProxyInstance(
                Terminal.class.getClassLoader(),
                new Class<?>[]{Terminal.class},
                handler);
    }

    /**
     * Invocation handler for stub Terminal.
     */
    private static class StubTerminalHandler implements InvocationHandler {

        private final PrintWriter writer;

        private boolean flushed;

        StubTerminalHandler(final PrintWriter writer) {
            this.writer = writer;
        }

        boolean isFlushed() {
            return flushed;
        }

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) {
            String methodName = method.getName();
            if ("writer".equals(methodName)) {
                return writer;
            } else if ("flush".equals(methodName)) {
                flushed = true;
                writer.flush();
                return null;
            } else if ("getName".equals(methodName)) {
                return "StubTerminal";
            } else if ("getWidth".equals(methodName)) {
                return 80;
            } else if ("getHeight".equals(methodName)) {
                return 24;
            } else if ("isAnsiSupported".equals(methodName)) {
                return false;
            } else if ("echo".equals(methodName)) {
                return false;
            } else if (method.getReturnType() == boolean.class) {
                return false;
            } else if (method.getReturnType() == int.class) {
                return 0;
            } else {
                return null;
            }
        }
    }
}
