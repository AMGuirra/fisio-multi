@echo off
setlocal EnableExtensions
title FisioGestao - Atualizar local
cd /d "%~dp0.."

rem ===============================================================
rem  Atualiza a aplicacao LOCAL (nao usa git):
rem  encerra a instancia na porta 8080, recompila e inicia de novo
rem  em segundo plano.
rem ===============================================================

set "PID_8080="
for /f "tokens=5" %%P in ('netstat -ano ^| findstr /r /c:":8080 .*LISTENING"') do if not defined PID_8080 set "PID_8080=%%P"
if defined PID_8080 (
    echo Encerrando instancia atual ^(PID %PID_8080%^)...
    taskkill /PID %PID_8080% /F >nul 2>&1
    if errorlevel 1 (
        echo [ERRO] Nao foi possivel encerrar o PID %PID_8080%.
        pause
        exit /b 1
    )
    timeout /t 3 /nobreak >nul
)

call "%~dp0iniciar-segundo-plano.bat" build
endlocal
exit /b %errorlevel%
