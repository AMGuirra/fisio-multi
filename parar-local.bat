@echo off
setlocal EnableExtensions EnableDelayedExpansion
title FisioGestao - Parar aplicacao local
cd /d "%~dp0"

rem ===============================================================
rem  Encerra SOMENTE o processo que esta escutando a porta 8080,
rem  apos mostrar o PID e pedir confirmacao.
rem ===============================================================

set "PID_8080="
for /f "tokens=5" %%P in ('netstat -ano ^| findstr /r /c:":8080 .*LISTENING"') do if not defined PID_8080 set "PID_8080=%%P"

if not defined PID_8080 (
    echo [INFO] Nenhum processo esta usando a porta 8080. Nada a encerrar.
    echo.
    pause
    endlocal
    exit /b 0
)

if "%PID_8080%"=="0" (
    echo [ERRO] PID invalido encontrado para a porta 8080. Nada foi encerrado.
    pause
    endlocal
    exit /b 1
)

set "NOME_PROCESSO=desconhecido"
for /f "tokens=1 delims=," %%N in ('tasklist /fi "PID eq %PID_8080%" /fo csv /nh 2^>nul') do set "NOME_PROCESSO=%%~N"

echo Processo usando a porta 8080:
echo   PID:      %PID_8080%
echo   Processo: %NOME_PROCESSO%
echo.
if /i not "%NOME_PROCESSO%"=="java.exe" (
    echo [AVISO] O processo nao e java.exe - pode nao ser o FisioGestao.
    echo.
)

choice /c SN /n /m "Deseja encerrar este processo? [S/N] "
if errorlevel 2 (
    echo [INFO] Operacao cancelada. Nada foi encerrado.
    echo.
    pause
    endlocal
    exit /b 0
)

taskkill /PID %PID_8080% /F >nul 2>&1
if errorlevel 1 (
    echo [ERRO] Nao foi possivel encerrar o PID %PID_8080%. Tente executar como administrador.
    echo.
    pause
    endlocal
    exit /b 1
)
echo [OK] Processo %PID_8080% encerrado. A porta 8080 foi liberada.
echo.
pause
endlocal
exit /b 0
