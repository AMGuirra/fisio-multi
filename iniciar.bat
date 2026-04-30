@echo off
title FisioGestao
cd /d "%~dp0"

echo ===============================================================
echo   FisioGestao - Build e Inicializacao
echo ===============================================================
echo.

:: ---------------------------------------------------------------
:: Java
:: ---------------------------------------------------------------
set "JAVA_CMD=java"
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
)

echo Verificando Java...
"%JAVA_CMD%" -version 2>&1 | findstr /i "version"
if errorlevel 1 (
    echo.
    echo [ERRO] Java nao encontrado. Configure JAVA_HOME para JDK 21+.
    goto :fim
)
echo.

:: ---------------------------------------------------------------
:: Banco de dados: carrega render-db.env se existir
:: ---------------------------------------------------------------
if exist "render-db.env" (
    echo Carregando configuracoes do banco [render-db.env]...
    for /f "usebackq delims=" %%L in ("render-db.env") do (
        call :set_env "%%L"
    )
)

if defined SPRING_DATASOURCE_URL (
    echo Banco: %SPRING_DATASOURCE_URL%
) else if defined DATABASE_URL (
    echo Banco Render: DATABASE_URL carregada
) else if defined PGHOST (
    echo Banco Render: %PGHOST%:%PGPORT%/%PGDATABASE%
) else (
    echo [AVISO] Banco Render nao configurado - usando banco local
)
echo.

:: ---------------------------------------------------------------
:: Build: sempre recompila para garantir codigo atualizado
:: ---------------------------------------------------------------
echo Compilando projeto...
echo.
"%JAVA_CMD%" -classpath ".mvn\wrapper\maven-wrapper.jar" "-Dmaven.multiModuleProjectDirectory=%CD%" org.apache.maven.wrapper.MavenWrapperMain clean package -DskipTests
if errorlevel 1 (
    echo.
    echo [ERRO] Build falhou.
    goto :fim
)
echo.

set "APP_JAR="
for %%F in ("target\*.jar") do (
    echo %%~nxF | findstr /b /c:"original-" >nul
    if errorlevel 1 if not defined APP_JAR set "APP_JAR=%%~fF"
)

if not defined APP_JAR (
    echo [ERRO] Build concluido, mas nenhum JAR executavel foi encontrado em target.
    goto :fim
)

:: ---------------------------------------------------------------
:: Iniciar aplicacao
:: ---------------------------------------------------------------
echo Iniciando aplicacao...
echo.
"%JAVA_CMD%" -jar "%APP_JAR%"
echo.
echo [INFO] Aplicacao encerrada (codigo: %errorlevel%).

:fim
echo.
echo Pressione qualquer tecla para fechar...
pause >nul
goto :eof

:: ---------------------------------------------------------------
:: Sub-rotina: set nativo de KEY=VALUE
:: ---------------------------------------------------------------
:set_env
set "ENVLINE=%~1"
if "%ENVLINE%"=="" goto :eof
if "%ENVLINE:~0,1%"=="#" goto :eof
set "%ENVLINE%"
goto :eof
