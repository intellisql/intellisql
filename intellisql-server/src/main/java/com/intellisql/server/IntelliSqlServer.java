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

package com.intellisql.server;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.calcite.avatica.remote.LocalService;
import org.apache.calcite.avatica.server.AvaticaProtobufHandler;
import org.apache.calcite.avatica.server.HttpServer;
import com.intellisql.kernel.IntelliSqlKernel;
import com.intellisql.kernel.metadata.MetadataManager;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * IntelliSql server implementation using Avatica HTTP Server. Supports Protobuf serialization.
 */
@Slf4j
@Getter
public class IntelliSqlServer {

    private static final int DEFAULT_PORT = 8765;

    private static final String DEFAULT_CONFIG_PATH = "conf/model.yaml";

    private final ServerConfig config;

    private final IntelliSqlMeta meta;

    private HttpServer server;

    private final AtomicBoolean running = new AtomicBoolean(false);

    private IntelliSqlKernel kernel;

    /**
     * Creates a new IntelliSqlServer with default configuration.
     */
    public IntelliSqlServer() {
        this(ServerConfig.defaultConfig());
    }

    /**
     * Creates a new IntelliSqlServer with the specified configuration.
     *
     * @param config the server configuration
     */
    public IntelliSqlServer(final ServerConfig config) {
        this.config = config;
        this.meta = new IntelliSqlMeta();
    }

    /**
     * Starts the IntelliSql server.
     *
     * @throws IllegalStateException if the server fails to start
     */
    public void start() {
        log.info("Starting IntelliSql server on port {}", config.getPort());
        try {
            // Initialize kernel and metadata from config
            initializeKernel();
            LocalService service = new LocalService(meta);
            AvaticaProtobufHandler handler = new AvaticaProtobufHandler(service);
            server = new HttpServer.Builder().withPort(config.getPort()).withHandler(handler).build();
            server.start();
            running.set(true);
            log.info("IntelliSql server started successfully on port {}", config.getPort());
        } catch (final IllegalStateException ex) {
            log.error("Failed to start IntelliSql server", ex);
            throw new IllegalStateException("Failed to start server", ex);
        }
    }

    /**
     * Initializes the IntelliSql kernel and metadata from configuration.
     */
    private void initializeKernel() {
        try {
            InputStream configStream = getClass().getClassLoader().getResourceAsStream(DEFAULT_CONFIG_PATH);
            if (configStream == null) {
                File configFile = new File(DEFAULT_CONFIG_PATH);
                if (configFile.exists()) {
                    configStream = Files.newInputStream(configFile.toPath());
                }
            }
            if (configStream != null) {
                log.info("Loading configuration from {}", DEFAULT_CONFIG_PATH);
                Path tempConfig = Files.createTempFile("intellisql-config", ".yaml");
                Files.copy(configStream, tempConfig, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                configStream.close();
                kernel = IntelliSqlKernel.create(tempConfig);
                kernel.initialize();
                MetadataManager metadataManager = kernel.getMetadataManager();
                meta.setMetadataManager(metadataManager);
                meta.setKernel(kernel);
                log.info("Kernel initialized with {} tables", metadataManager.getAllTables().size());
                Files.deleteIfExists(tempConfig);
            } else {
                log.warn("Configuration file not found at {}, using empty metadata", DEFAULT_CONFIG_PATH);
            }
            // CHECKSTYLE:OFF
        } catch (final Exception ex) {
            // CHECKSTYLE:ON
            log.warn("Failed to initialize kernel: {}. Server will start with empty metadata.", ex.getMessage());
        }
    }

    /**
     * Stops the IntelliSql server.
     */
    public void stop() {
        if (server != null) {
            log.info("Stopping IntelliSql server");
            server.stop();
            running.set(false);
            if (kernel != null) {
                kernel.close();
            }
            log.info("IntelliSql server stopped");
        }
    }

    /**
     * Returns the port the server is listening on.
     *
     * @return the server port
     */
    public int getPort() {
        return server != null ? server.getPort() : config.getPort();
    }

    /**
     * Checks if the server is currently running.
     *
     * @return true if the server is running, false otherwise
     */
    public boolean isRunning() {
        return running.get() && server != null;
    }

    /**
     * Command-line entry point for starting the IntelliSql server.
     *
     * @param args command line arguments, first argument can be port number
     */
    // CHECKSTYLE:OFF: UncommentedMain
    // This is the main entry point for the server application
    public static void main(final String[] args) {
        // CHECKSTYLE:ON: UncommentedMain
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (final NumberFormatException ex) {
                log.warn("Invalid port number: {}, using default {}", args[0], DEFAULT_PORT);
            }
        }
        final IntelliSqlServer server = new IntelliSqlServer(ServerConfig.builder().port(port).build());
        Runtime.getRuntime()
                .addShutdownHook(
                        new Thread(
                                () -> {
                                    log.info("Shutdown hook triggered");
                                    server.stop();
                                }));
        server.start();
        log.info("IntelliSql server running. Press Ctrl+C to stop.");
        try {
            while (server.isRunning()) {
                Thread.sleep(1000);
            }
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
