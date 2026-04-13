#!/bin/bash
#
# Licensed to the IntelliSql Project under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The IntelliSql Project licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Determine the directory where this script is located
BIN_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
BASE_DIR="$(cd "$BIN_DIR/.." >/dev/null 2>&1 && pwd)"
LIB_DIR="$BASE_DIR/lib"

# Check if Java is installed
if ! command -v java >/dev/null 2>&1; then
    echo "Error: Java is not installed or not in PATH." >&2
    exit 1
fi

# Build classpath
CLASSPATH="$LIB_DIR/*"

# Execute IntelliSqlClient
exec java -cp "$CLASSPATH" com.intellisql.client.IntelliSqlClient "$@"
