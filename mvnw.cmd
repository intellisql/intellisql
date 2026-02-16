@echo off
@REM Maven Wrapper script for Windows
setlocal
set MAVEN_PROJECTBASEDIR=%~dp0
set WRAPPER_JAR="%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar"
set DEFAULT_JVM_OPTS="-Djava.net.preferIPv4Stack=true"
if not "%JAVA_HOME%"=="" goto useJavaHome
set JAVA_EXE=java.exe
goto runMaven
:useJavaHome
set JAVA_EXE="%JAVA_HOME%\bin\java.exe"
:runMaven
%JAVA_EXE% %DEFAULT_JVM_OPTS% -classpath %WRAPPER_JAR% org.apache.maven.wrapper.MavenWrapperMain %*
