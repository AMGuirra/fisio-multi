@echo off
setlocal EnableExtensions
title FisioGestao - Diagnostico local
cd /d "%~dp0"

rem ===============================================================
rem  Diagnostico do ambiente local (somente leitura, nada e alterado).
rem ===============================================================

set "DB_HOST=localhost"
set "DB_PORT=5432"
set "DB_NAME=fisio"
set "DB_USER=postgres"
if defined FISIO_DB_PASSWORD (set "PGPASSWORD=%FISIO_DB_PASSWORD%") else (set "PGPASSWORD=postgres")
set "PGCONNECT_TIMEOUT=5"
set "PROBLEMAS=0"

echo ===============================================================
echo   FisioGestao - Diagnostico do ambiente local
echo ===============================================================
echo.

rem --- 1. Java ----------------------------------------------------------
set "JAVA_CMD=java"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
"%JAVA_CMD%" -version >nul 2>&1
if errorlevel 1 (
    echo [FALHA] Java: nao encontrado.
    echo         Correcao: instale o JDK 26 e defina JAVA_HOME ^(raiz do JDK, sem \bin^).
    set /a PROBLEMAS+=1
) else (
    for /f "tokens=*" %%V in ('call "%JAVA_CMD%" -version 2^>^&1 ^| findstr /i version') do echo [OK]    Java: %%V
)
if defined JAVA_HOME (echo         JAVA_HOME=%JAVA_HOME%) else (echo         JAVA_HOME nao definido ^(usando java do PATH^))

rem --- 2. Maven Wrapper -------------------------------------------------
if exist "%~dp0mvnw.cmd" if exist "%~dp0.mvn\wrapper\maven-wrapper.properties" (
    echo [OK]    Maven Wrapper: mvnw.cmd encontrado.
    goto :postgres
)
echo [FALHA] Maven Wrapper: mvnw.cmd ou .mvn\wrapper ausente em "%~dp0".
set /a PROBLEMAS+=1

:postgres
rem --- 3. PostgreSQL (porta) --------------------------------------------
powershell -NoProfile -Command "$c=New-Object Net.Sockets.TcpClient; try { if ($c.ConnectAsync('%DB_HOST%',%DB_PORT%).Wait(3000) -and $c.Connected) { exit 0 } else { exit 1 } } catch { exit 1 } finally { $c.Close() }" >nul 2>&1
if errorlevel 1 (
    echo [FALHA] PostgreSQL: nada respondendo em %DB_HOST%:%DB_PORT%.
    echo         Correcao: inicie o servico do PostgreSQL ^(services.msc^).
    set /a PROBLEMAS+=1
    goto :porta
)
echo [OK]    PostgreSQL: respondendo em %DB_HOST%:%DB_PORT%.

rem --- 4. psql, login e banco -------------------------------------------
where psql >nul 2>&1
if errorlevel 1 (
    echo [AVISO] psql nao esta no PATH: nao foi possivel validar login e banco "%DB_NAME%".
    echo         Correcao: adicione C:\Program Files\PostgreSQL\^<versao^>\bin ao PATH.
    goto :porta
)
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d postgres -w -tAc "SELECT 1" >nul 2>&1
if errorlevel 1 (
    echo [FALHA] Login no PostgreSQL com o usuario %DB_USER% falhou.
    echo         Correcao: confira a senha ^(padrao local "postgres" ou variavel FISIO_DB_PASSWORD^).
    set /a PROBLEMAS+=1
    goto :porta
)
echo [OK]    Login no PostgreSQL com o usuario %DB_USER%.
set "EXISTE="
for /f "usebackq delims=" %%R in (`psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d postgres -w -tAc "SELECT 1 FROM pg_database WHERE datname = '%DB_NAME%'"`) do set "EXISTE=%%R"
if not "%EXISTE%"=="1" (
    echo [FALHA] Banco "%DB_NAME%" nao existe.
    echo         Correcao: execute criar-banco-local.bat
    set /a PROBLEMAS+=1
    goto :porta
)
echo [OK]    Banco "%DB_NAME%" existe.
set "VERSAO="
for /f "usebackq delims=" %%R in (`psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -w -tAc "SELECT max(version::int) FROM flyway_schema_history_fisiogestao WHERE success" 2^>nul`) do set "VERSAO=%%R"
if defined VERSAO (
    echo         Migrations Flyway aplicadas ate a versao V%VERSAO%.
) else (
    echo         Nenhuma migration aplicada ainda ^(o Flyway roda ao iniciar a aplicacao^).
)

:porta
rem --- 5. Porta 8080 ----------------------------------------------------
set "PID_8080="
for /f "tokens=5" %%P in ('netstat -ano ^| findstr /r /c:":8080 .*LISTENING"') do if not defined PID_8080 set "PID_8080=%%P"
if defined PID_8080 (
    set "NOME_PROCESSO=desconhecido"
    for /f "tokens=1 delims=," %%N in ('tasklist /fi "PID eq %PID_8080%" /fo csv /nh 2^>nul') do set "NOME_PROCESSO=%%~N"
    call echo [INFO]  Porta 8080 em uso pelo PID %PID_8080% ^(%%NOME_PROCESSO%%^) - aplicacao possivelmente rodando: http://localhost:8080
    echo         Para encerrar: parar-local.bat
) else (
    echo [OK]    Porta 8080 livre ^(aplicacao parada^). Para iniciar: iniciar-local.bat
)

echo.
echo ---------------------------------------------------------------
if "%PROBLEMAS%"=="0" (
    echo   Diagnostico: ambiente pronto.
) else (
    echo   Diagnostico: %PROBLEMAS% problema^(s^) encontrado^(s^). Veja as correcoes acima.
)
echo   Acesso local: http://localhost:8080  ^(admin / admin^)
echo ---------------------------------------------------------------
echo.
pause
if "%PROBLEMAS%"=="0" (endlocal & exit /b 0) else (endlocal & exit /b 1)
