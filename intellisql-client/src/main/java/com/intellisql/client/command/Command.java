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

/** Interface for executable commands in the CLI. */
public interface Command {

    /**
     * Executes the command.
     *
     * @throws ClientException if execution fails
     * @throws Exception if unexpected error occurs
     */
    void execute() throws ClientException, Exception;

    /**
     * Returns a description of the command.
     *
     * @return command description
     */
    default String getDescription() {
        return this.getClass().getSimpleName();
    }
}
