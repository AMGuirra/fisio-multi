@echo off
title Reiniciando FisioGestao
cd /d "%~dp0"

echo ===============================================================
echo   FisioGestao - Reinicializacao
echo ===============================================================
echo.

:: ---------------------------------------------------------------
:: Encerra processos java da aplicacao atual
:: ---------------------------------------------------------------
echo Encerrando instancia anterior...

wmic process where "name='java.exe' and (commandline like '%%fisiogestao%%' or commandline like '%%finpessoal%%')" call terminate >nul 2>&1
timeout /t 2 /nobreak >nul
echo Encerramento concluido.
echo.

:: ---------------------------------------------------------------
:: Inicia novamente
:: ---------------------------------------------------------------
echo Reiniciando...
echo.
call "%~dp0iniciar.bat"
