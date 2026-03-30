@echo off
setlocal

set "WORKSPACE_DIR=%~dp0.."
set "JAVA_HOME=C:\Program Files\Java\jdk-25.0.2"
set "PATH=%JAVA_HOME%\bin;%PATH%"
cd /d "%WORKSPACE_DIR%"

call ".\mvnw.cmd" -q -DskipTests spring-boot:run
