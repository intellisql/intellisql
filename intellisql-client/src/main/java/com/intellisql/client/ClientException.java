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

package com.intellisql.client;

/** Custom exception for client operations. */
public class ClientException extends Exception {

    /**
     * Creates a new ClientException with a message.
     *
     * @param message the error message
     */
    public ClientException(final String message) {
        super(message);
    }

    /**
     * Creates a new ClientException with a message and cause.
     *
     * @param message the error message
     * @param cause the underlying cause
     */
    public ClientException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
