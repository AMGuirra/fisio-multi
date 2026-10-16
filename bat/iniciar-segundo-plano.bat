@echo off
setlocal EnableExtensions
title FisioGestao - Iniciar local em segundo plano
cd /d "%~dp0.."
set "PROJECT_ROOT=%CD%"

rem ===============================================================
rem  Inicia o FisioGestao LOCAL em segundo plano (janela oculta):
rem    http://localhost:8080  -  PostgreSQL localhost:5432/fisio
rem  Nao usa render-db.env nem qualquer banco/configuracao do Render.
rem  Pode fechar este terminal: a aplicacao continua rodando.
rem  Logs: logs\fisiogestao.out.log / logs\fisiogestao.err.log
rem  Para encerrar: parar-local.bat
rem  Para recompilar antes de iniciar: iniciar-segundo-plano.bat build
rem ===============================================================

set "DB_HOST=localhost"
set "DB_PORT=5432"
set "DB_NAME=fisio"
set "SPRING_DATASOURCE_USERNAME=postgres"
if defined FISIO_DB_PASSWORD (set "SPRING_DATASOURCE_PASSWORD=%FISIO_DB_PASSWORD%") else (set "SPRING_DATASOURCE_PASSWORD=postgres")
set "SPRING_DATASOURCE_URL=jdbc:postgresql://%DB_HOST%:%DB_PORT%/%DB_NAME%?sslmode=disable"
set "SERVER_PORT=8080"
set "APP_BASE_URL=http://localhost:8080"
set "SERVER_SERVLET_SESSION_PERSISTENT=false"
set "APP_BOOTSTRAP_SUPER_ADMIN_ENABLED=true"
set "APP_BOOTSTRAP_SUPER_ADMIN_USERNAME=admin"
set "APP_BOOTSTRAP_SUPER_ADMIN_PASSWORD=admin"

rem Impede que configuracao de producao/Render herdada do sistema desvie o banco.
set "DATABASE_URL="
set "PGHOST="
set "PGPORT="
set "PGDATABASE="
set "RENDER="
set "RENDER_EXTERNAL_URL="
set "RENDER_EXTERNAL_HOSTNAME="
set "SPRING_PROFILES_ACTIVE="
set "PORT="

rem --- chave de criptografia local ---
set "ARQUIVO_CHAVE=%PROJECT_ROOT%\.local-dev\app-encryption.key"
if not defined APP_ENCRYPTION_KEY (
    if not exist "%ARQUIVO_CHAVE%" (
        if not exist "%PROJECT_ROOT%\.local-dev" mkdir "%PROJECT_ROOT%\.local-dev"
        powershell -NoProfile -Command "$b=New-Object byte[] 48; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [IO.File]::WriteAllText('%ARQUIVO_CHAVE%', [Convert]::ToBase64String($b))"
    )
    set /p APP_ENCRYPTION_KEY=<"%ARQUIVO_CHAVE%"
)
if not defined APP_ENCRYPTION_KEY (
    echo [ERRO] Chave de criptografia local indisponivel em .local-dev\app-encryption.key
    pause
    exit /b 1
)

rem --- porta 8080 livre? ---
set "PID_8080="
for /f "tokens=5" %%P in ('netstat -ano ^| findstr /r /c:":8080 .*LISTENING"') do if not defined PID_8080 set "PID_8080=%%P"
if defined PID_8080 (
    echo [AVISO] A porta 8080 ja esta em uso ^(PID %PID_8080%^). A aplicacao ja deve estar rodando.
    echo         http://localhost:8080  ^|  Para encerrar: parar-local.bat
    pause
    exit /b 1
)

rem --- PostgreSQL local acessivel? ---
powershell -NoProfile -Command "$c=New-Object Net.Sockets.TcpClient; try { if ($c.ConnectAsync('%DB_HOST%',%DB_PORT%).Wait(3000) -and $c.Connected) { exit 0 } else { exit 1 } } catch { exit 1 } finally { $c.Close() }" >nul 2>&1
if errorlevel 1 (
    echo [ERRO] PostgreSQL nao responde em %DB_HOST%:%DB_PORT%. Inicie o servico ^(services.msc^).
    pause
    exit /b 1
)

set "JAVA_CMD=java"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"

set "APP_JAR="
if /i not "%~1"=="build" call :achar_jar

if not defined APP_JAR (
    echo Compilando projeto...
    "%JAVA_CMD%" -classpath ".mvn\wrapper\maven-wrapper.jar" "-Dmaven.multiModuleProjectDirectory=%CD%" org.apache.maven.wrapper.MavenWrapperMain clean package -DskipTests
    if errorlevel 1 ( echo [ERRO] Build falhou. & pause & exit /b 1 )
    call :achar_jar
)
if not defined APP_JAR ( echo [ERRO] Nenhum JAR encontrado em target. & pause & exit /b 1 )

if not exist "logs" mkdir "logs"

powershell -NoProfile -Command "Start-Process -FilePath '%JAVA_CMD%' -ArgumentList '-jar','\"%APP_JAR%\"' -WorkingDirectory '%CD%' -WindowStyle Hidden -RedirectStandardOutput 'logs\fisiogestao.out.log' -RedirectStandardError 'logs\fisiogestao.err.log'"
if errorlevel 1 ( echo [ERRO] Nao foi possivel iniciar a aplicacao. & pause & exit /b 1 )

echo [OK] FisioGestao ^(local^) iniciado em segundo plano.
echo      Aguarde alguns segundos e acesse http://localhost:8080  ^(admin / admin^)
echo      Logs: %CD%\logs\fisiogestao.out.log
timeout /t 5 >nul
endlocal
exit /b 0

:achar_jar
for %%F in ("target\*.jar") do (
    echo %%~nxF | findstr /b /c:"original-" >nul
    if errorlevel 1 if not defined APP_JAR set "APP_JAR=%%~fF"
)
goto :eof
