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
# IntelliSQL Server Stop Script
# ============================================================================

set -e

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_HOME="$(cd "${SCRIPT_DIR}/.." && pwd)"

APP_NAME="intellisql-server"
PID_FILE="${PROJECT_HOME}/${APP_NAME}.pid"

# Default timeout in seconds
TIMEOUT=30

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --force|-f)
            FORCE=1
            shift
            ;;
        --timeout|-t)
            TIMEOUT="$2"
            shift 2
            ;;
        --help|-h)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --force, -f       Force kill the server"
            echo "  --timeout, -t N   Timeout in seconds (default: 30)"
            echo "  --help, -h        Show this help message"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

# Check if PID file exists
if [ ! -f "$PID_FILE" ]; then
    echo "IntelliSQL Server is not running (no PID file found)"
    exit 0
fi

# Read PID
PID=$(cat "$PID_FILE")

# Check if process is running
if ! ps -p "$PID" > /dev/null 2>&1; then
    echo "IntelliSQL Server is not running (stale PID file)"
    rm -f "$PID_FILE"
    exit 0
fi

echo "Stopping IntelliSQL Server (PID: $PID)..."

# Send SIGTERM for graceful shutdown
kill "$PID" 2>/dev/null || true

# Wait for process to terminate
COUNTER=0
while ps -p "$PID" > /dev/null 2>&1; do
    sleep 1
    COUNTER=$((COUNTER + 1))

    if [ "$COUNTER" -ge "$TIMEOUT" ]; then
        echo "Server did not stop gracefully within ${TIMEOUT} seconds"
        if [ "${FORCE:-0}" -eq 1 ]; then
            echo "Force killing server..."
            kill -9 "$PID" 2>/dev/null || true
            break
        else
            echo "Use --force to force kill the server"
            exit 1
        fi
    fi

    # Print progress every 5 seconds
    if [ $((COUNTER % 5)) -eq 0 ]; then
        echo "Waiting for server to stop... (${COUNTER}s)"
    fi
done

# Clean up PID file
rm -f "$PID_FILE"

echo "IntelliSQL Server stopped successfully"
