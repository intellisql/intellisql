@echo off
rem
rem Licensed to the IntelliSql Project under one or more
rem contributor license agreements.  See the NOTICE file distributed with
rem this work for additional information regarding copyright ownership.
rem The IntelliSql Project licenses this file to You under the Apache License, Version 2.0
rem (the "License"); you may not use this file except in compliance with
rem the License.  You may obtain a copy of the License at
rem
rem     http://www.apache.org/licenses/LICENSE-2.0
rem
rem Unless required by applicable law or agreed to in writing, software
rem distributed under the License is distributed on an "AS IS" BASIS,
rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
rem See the License for the specific language governing permissions and
rem limitations under the License.
rem

setlocal

rem Determine the directory where this script is located
set "BIN_DIR=%~dp0"
set "BASE_DIR=%BIN_DIR%.."
set "LIB_DIR=%BASE_DIR%\lib"

rem Check if Java is installed
java -version >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo Error: Java is not installed or not in PATH. >&2
    exit /b 1
)

rem Build classpath
set "CLASSPATH=%LIB_DIR%\*"

rem Execute IntelliSqlClient
java -cp "%CLASSPATH%" com.intellisql.client.IntelliSqlClient %*

endlocal
