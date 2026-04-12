#!/bin/bash
#
# Licensed to the IntelliSQL Project under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The IntelliSQL Project licenses this file to You under the Apache License, Version 2.0
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

# ============================================================================
# IntelliSQL Server Startup Script
# ============================================================================

set -e

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_HOME="$(cd "${SCRIPT_DIR}/.." && pwd)"

# Default configuration
APP_NAME="intellisql-server"
MAIN_CLASS="org.intellisql.server.IntelliSQLServer"
CONF_DIR="${PROJECT_HOME}/conf"
LIB_DIR="${PROJECT_HOME}/lib"
LOG_DIR="${PROJECT_HOME}/logs"
PID_FILE="${PROJECT_HOME}/${APP_NAME}.pid"

# Default JVM options
JVM_OPTS="${JVM_OPTS:-}"
HEAP_SIZE="${HEAP_SIZE:-2g}"
METASPACE_SIZE="${METASPACE_SIZE:-256m}"
MAX_METASPACE_SIZE="${MAX_METASPACE_SIZE:-512m}"

# JVM GC options
GC_OPTS="${GC_OPTS:--XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:InitiatingHeapOccupancyPercent=35}"

# Debug mode (set to 1 to enable remote debugging)
DEBUG_MODE=0
DEBUG_PORT=5005

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --debug)
            DEBUG_MODE=1
            shift
            ;;
        --debug-port)
            DEBUG_PORT="$2"
            shift 2
            ;;
        --config)
            CONF_DIR="$2"
            shift 2
            ;;
        --heap)
            HEAP_SIZE="$2"
            shift 2
            ;;
        --help|-h)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --debug           Enable remote debugging"
            echo "  --debug-port PORT Specify debug port (default: 5005)"
            echo "  --config DIR      Specify configuration directory"
            echo "  --heap SIZE       Specify heap size (default: 2g)"
            echo "  --help, -h        Show this help message"
            echo ""
            echo "Environment variables:"
            echo "  JVM_OPTS          Additional JVM options"
            echo "  HEAP_SIZE         JVM heap size (default: 2g)"
            echo "  METASPACE_SIZE    Initial metaspace size (default: 256m)"
            echo "  MAX_METASPACE_SIZE Maximum metaspace size (default: 512m)"
            echo "  GC_OPTS           GC options (default: G1GC settings)"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

# Check if server is already running
if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if ps -p "$PID" > /dev/null 2>&1; then
        echo "IntelliSQL Server is already running (PID: $PID)"
        exit 1
    else
        # Stale PID file, remove it
        rm -f "$PID_FILE"
    fi
fi

# Create log directory if it doesn't exist
mkdir -p "$LOG_DIR"

# Build classpath
CLASSPATH="${CONF_DIR}"
if [ -d "$LIB_DIR" ]; then
    for jar in "${LIB_DIR}"/*.jar; do
        if [ -f "$jar" ]; then
            CLASSPATH="${CLASSPATH}:${jar}"
        fi
    done
fi

# Build JVM arguments
JAVA_OPTS=(
    "-Xms${HEAP_SIZE}"
    "-Xmx${HEAP_SIZE}"
    "-XX:MetaspaceSize=${METASPACE_SIZE}"
    "-XX:MaxMetaspaceSize=${MAX_METASPACE_SIZE}"
    ${GC_OPTS}
    "-Dlogback.configurationFile=${CONF_DIR}/logback.xml"
    "-Dconfig.file=${CONF_DIR}/model.yaml"
    "-Dapp.home=${PROJECT_HOME}"
    "-Dapp.name=${APP_NAME}"
    "-Dapp.pid=$$"
    "-Djava.awt.headless=true"
    "-Dfile.encoding=UTF-8"
    "-Djava.security.egd=file:/dev/./urandom"
)

# Add debug options if enabled
if [ "$DEBUG_MODE" -eq 1 ]; then
    JAVA_OPTS+=(
        "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:${DEBUG_PORT}"
    )
    echo "Remote debugging enabled on port ${DEBUG_PORT}"
fi

# Add user-defined JVM options
if [ -n "$JVM_OPTS" ]; then
    JAVA_OPTS+=($JVM_OPTS)
fi

# Print startup banner
echo "=========================================="
echo "  Starting IntelliSQL Server"
echo "=========================================="
echo "  Project Home: ${PROJECT_HOME}"
echo "  Configuration: ${CONF_DIR}"
echo "  Heap Size: ${HEAP_SIZE}"
echo "  Log Directory: ${LOG_DIR}"
if [ "$DEBUG_MODE" -eq 1 ]; then
    echo "  Debug Port: ${DEBUG_PORT}"
fi
echo "=========================================="

# Start the server
nohup java "${JAVA_OPTS[@]}" -cp "${CLASSPATH}" "${MAIN_CLASS}" > "${LOG_DIR}/console.log" 2>&1 &
SERVER_PID=$!

# Save PID to file
echo $SERVER_PID > "$PID_FILE"

echo "IntelliSQL Server started (PID: $SERVER_PID)"
echo "Logs available at: ${LOG_DIR}"

# Wait briefly and check if process is still running
sleep 2
if ps -p "$SERVER_PID" > /dev/null 2>&1; then
    echo "Server started successfully"
else
    echo "Server failed to start. Check logs at: ${LOG_DIR}/console.log"
    rm -f "$PID_FILE"
    exit 1
fi
