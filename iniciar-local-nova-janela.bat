@echo off
setlocal EnableExtensions
cd /d "%~dp0"

rem ===============================================================
rem  Abre o FisioGestao local em uma nova janela do terminal.
rem  Nao abre uma segunda instancia se a porta 8080 ja estiver em uso.
rem ===============================================================

set "PID_8080="
for /f "tokens=5" %%P in ('netstat -ano ^| findstr /r /c:":8080 .*LISTENING"') do if not defined PID_8080 set "PID_8080=%%P"
if defined PID_8080 (
    echo [AVISO] A porta 8080 ja esta em uso ^(PID %PID_8080%^).
    echo         A aplicacao provavelmente ja esta rodando: http://localhost:8080
    echo         Para encerrar a instancia atual, execute parar-local.bat
    echo.
    pause
    endlocal
    exit /b 1
)

if not exist "%~dp0iniciar-local.bat" (
    echo [ERRO] iniciar-local.bat nao encontrado em "%~dp0".
    pause
    endlocal
    exit /b 1
)

start "FisioGestao - Local (http://localhost:8080)" /D "%~dp0" cmd /k ""%~dp0iniciar-local.bat""
echo [OK] FisioGestao iniciando em uma nova janela.
echo      Aguarde a mensagem "Started FisioGestaoApplication" e acesse http://localhost:8080
endlocal
exit /b 0
