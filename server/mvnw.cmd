@REM Licensed to the Apache Software Foundation (ASF)
@REM under one or more contributor license agreements.

@echo off

setlocal enabledelayedexpansion

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

set JAVACMD=java
if not "%JAVA_HOME%" == "" (
  set JAVACMD=%JAVA_HOME%\bin\java
)

if not exist "%JAVACMD%" (
  echo Error: JAVA_HOME is not set and no 'java' command could be found in your PATH.
  echo.
  echo Please set the JAVA_HOME variable in your environment to match the
  echo location of your Java installation.
  exit /b 1
)

set MAVEN_CMD_LINE_ARGS=%*

"%JAVACMD%" -classpath "%DIRNAME%.mvn\wrapper\maven-wrapper.jar" "-Dmaven.multiModuleProjectDirectory=%APP_HOME%" org.apache.maven.wrapper.MavenWrapperMain %MAVEN_CMD_LINE_ARGS%
if %ERRORLEVEL% neq 0 (
  exit /b %ERRORLEVEL%
)

endlocal
