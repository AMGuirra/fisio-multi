@echo off
setlocal EnableExtensions
title FisioGestao - Local (http://localhost:8080)
cd /d "%~dp0"

rem ===============================================================
rem  Inicia o FisioGestao localmente:
rem    http://localhost:8080  -  PostgreSQL localhost:5432/fisio
rem  As migrations Flyway rodam automaticamente na inicializacao.
rem  Nao usa Docker e nao executa comandos destrutivos.
rem ===============================================================

set "DB_HOST=localhost"
set "DB_PORT=5432"
set "DB_NAME=fisio"
set "DB_SSLMODE=disable"
set "SPRING_DATASOURCE_USERNAME=postgres"
if defined FISIO_DB_PASSWORD (set "SPRING_DATASOURCE_PASSWORD=%FISIO_DB_PASSWORD%") else (set "SPRING_DATASOURCE_PASSWORD=postgres")
set "SPRING_DATASOURCE_URL=jdbc:postgresql://%DB_HOST%:%DB_PORT%/%DB_NAME%?sslmode=%DB_SSLMODE%"
set "SERVER_PORT=8080"
set "APP_BASE_URL=http://localhost:8080"

rem O DevTools grava sessoes em uma pasta temporaria fixa do Windows; se ela foi
rem criada por outro usuario (ex.: janela comum x "Executar como administrador"),
rem o Spring Boot recusa iniciar. Sessoes em memoria evitam esse conflito.
set "SERVER_SERVLET_SESSION_PERSISTENT=false"

rem Chave que criptografa as senhas SMTP salvas no banco. Precisa ser sempre a
rem mesma: e gerada uma unica vez (aleatoria) em .local-dev\app-encryption.key,
rem arquivo fora do Git. Se APP_ENCRYPTION_KEY ja estiver definida, ela e usada.
set "ARQUIVO_CHAVE=%~dp0.local-dev\app-encryption.key"
if defined APP_ENCRYPTION_KEY goto :chave_ok
if not exist "%ARQUIVO_CHAVE%" (
    if not exist "%~dp0.local-dev" mkdir "%~dp0.local-dev"
    powershell -NoProfile -Command "$b=New-Object byte[] 48; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [IO.File]::WriteAllText('%ARQUIVO_CHAVE%', [Convert]::ToBase64String($b))"
    if errorlevel 1 (
        echo [ERRO] Nao foi possivel gerar a chave local de criptografia em .local-dev.
        goto :falha
    )
    echo [OK] Chave local de criptografia gerada em .local-dev\app-encryption.key ^(nao versionada^).
)
set /p APP_ENCRYPTION_KEY=<"%ARQUIVO_CHAVE%"
if not defined APP_ENCRYPTION_KEY (
    echo [ERRO] O arquivo .local-dev\app-encryption.key esta vazio. Restaure o arquivo original
    echo        ^(senhas SMTP ja salvas dependem dele^) ou apague-o para gerar uma nova chave.
    goto :falha
)
:chave_ok

rem Superadmin admin/admin: SOMENTE ambiente local. Nao sobrescreve a senha
rem de um usuario "admin" que ja exista no banco.
set "APP_BOOTSTRAP_SUPER_ADMIN_ENABLED=true"
set "APP_BOOTSTRAP_SUPER_ADMIN_USERNAME=admin"
set "APP_BOOTSTRAP_SUPER_ADMIN_PASSWORD=admin"

rem Garante que nenhuma configuracao de producao/Render herdada do sistema
rem desvie a aplicacao para outro banco (somente nesta janela).
set "DATABASE_URL="
set "RENDER="
set "RENDER_EXTERNAL_URL="
set "RENDER_EXTERNAL_HOSTNAME="
set "SPRING_PROFILES_ACTIVE="
set "PORT="

echo ===============================================================
echo   FisioGestao - Execucao local
echo ===============================================================
echo.

rem --- Java -------------------------------------------------------------
set "JAVA_CMD=java"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
"%JAVA_CMD%" -version >nul 2>&1
if errorlevel 1 (
    echo [ERRO] Java nao encontrado.
    echo        Instale o JDK 26 e defina JAVA_HOME, por exemplo:
    echo          set JAVA_HOME=C:\Program Files\Java\jdk-26
    goto :falha
)
for /f "tokens=*" %%V in ('call "%JAVA_CMD%" -version 2^>^&1 ^| findstr /i version') do echo [OK] Java: %%V

rem --- Maven Wrapper ----------------------------------------------------
if not exist "%~dp0mvnw.cmd" (
    echo [ERRO] mvnw.cmd nao encontrado em "%~dp0".
    goto :falha
)
echo [OK] Maven Wrapper encontrado.

rem --- porta 8080 livre? ------------------------------------------------
set "PID_8080="
for /f "tokens=5" %%P in ('netstat -ano ^| findstr /r /c:":8080 .*LISTENING"') do if not defined PID_8080 set "PID_8080=%%P"
if defined PID_8080 (
    echo [ERRO] A porta 8080 ja esta em uso pelo processo PID %PID_8080%.
    echo        A aplicacao pode ja estar rodando: acesse http://localhost:8080
    echo        Para encerrar, execute parar-local.bat
    goto :falha
)
echo [OK] Porta 8080 livre.

rem --- PostgreSQL acessivel? --------------------------------------------
powershell -NoProfile -Command "$c=New-Object Net.Sockets.TcpClient; try { if ($c.ConnectAsync('%DB_HOST%',%DB_PORT%).Wait(3000) -and $c.Connected) { exit 0 } else { exit 1 } } catch { exit 1 } finally { $c.Close() }" >nul 2>&1
if errorlevel 1 (
    echo [ERRO] PostgreSQL nao responde em %DB_HOST%:%DB_PORT%.
    echo        Inicie o servico do PostgreSQL ^(services.msc^) e tente novamente.
    goto :falha
)
echo [OK] PostgreSQL respondendo em %DB_HOST%:%DB_PORT%.

rem --- banco existe? (requer psql; sem psql apenas avisa) ---------------
where psql >nul 2>&1
if errorlevel 1 (
    echo [AVISO] psql nao esta no PATH: nao foi possivel confirmar se o banco "%DB_NAME%" existe.
    echo         Se a inicializacao falhar com "database does not exist", execute criar-banco-local.bat
    goto :iniciar
)
set "PGPASSWORD=%SPRING_DATASOURCE_PASSWORD%"
set "PGCONNECT_TIMEOUT=5"
set "EXISTE="
for /f "usebackq delims=" %%R in (`psql -h %DB_HOST% -p %DB_PORT% -U %SPRING_DATASOURCE_USERNAME% -d postgres -w -tAc "SELECT 1 FROM pg_database WHERE datname = '%DB_NAME%'" 2^>nul`) do set "EXISTE=%%R"
set "PGPASSWORD="
if not "%EXISTE%"=="1" (
    echo [ERRO] O banco "%DB_NAME%" nao existe ^(ou usuario/senha do PostgreSQL incorretos^).
    echo        Execute primeiro: criar-banco-local.bat
    goto :falha
)
echo [OK] Banco "%DB_NAME%" encontrado.

:iniciar
echo.
echo ---------------------------------------------------------------
echo   URL:    http://localhost:8080
echo   Login:  admin   ^(deixe "Codigo da clinica" em branco^)
echo   Senha:  admin
echo   Banco:  jdbc:postgresql://%DB_HOST%:%DB_PORT%/%DB_NAME%
echo   Para parar: Ctrl+C nesta janela ou parar-local.bat
echo ---------------------------------------------------------------
echo.

call "%~dp0mvnw.cmd" spring-boot:run
set "SAIDA=%ERRORLEVEL%"
echo.
if not "%SAIDA%"=="0" (
    echo [ERRO] A aplicacao terminou com erro ^(codigo %SAIDA%^). Veja as mensagens acima.
    echo        Se voce encerrou a aplicacao com parar-local.bat, esta mensagem e esperada.
    goto :falha
)
echo [INFO] Aplicacao encerrada. Se ela nao chegou a iniciar ^(sem "Started FisioGestaoApplication"^),
echo        veja o erro acima ^(linha "Caused by"^) ou rode verificar-local.bat
pause
endlocal
exit /b 0

:falha
echo.
echo Pressione qualquer tecla para fechar...
pause >nul
endlocal
exit /b 1
