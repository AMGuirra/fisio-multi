@echo off
setlocal EnableExtensions
title FisioGestao - Criar banco local
cd /d "%~dp0"

rem ===============================================================
rem  Cria o banco PostgreSQL local "fisio" SOMENTE se ele nao existir.
rem  Nunca apaga, recria ou sobrescreve um banco existente.
rem  As tabelas sao criadas pelo Flyway ao iniciar a aplicacao.
rem
rem  Senha: usa a variavel FISIO_DB_PASSWORD se definida; senao, o
rem  padrao local "postgres". A senha nao e exibida na tela.
rem ===============================================================

set "DB_HOST=localhost"
set "DB_PORT=5432"
set "DB_NAME=fisio"
set "DB_USER=postgres"
if defined FISIO_DB_PASSWORD (set "PGPASSWORD=%FISIO_DB_PASSWORD%") else (set "PGPASSWORD=postgres")
set "PGCONNECT_TIMEOUT=5"

echo ===============================================================
echo   FisioGestao - Criar banco local
echo   Servidor: %DB_HOST%:%DB_PORT%   Banco: %DB_NAME%   Usuario: %DB_USER%
echo ===============================================================
echo.

rem --- psql precisa estar no PATH -------------------------------------
where psql >nul 2>&1
if errorlevel 1 (
    echo [ERRO] O comando "psql" nao foi encontrado no PATH.
    echo.
    echo  Como resolver:
    echo   1. Instale o PostgreSQL: https://www.postgresql.org/download/windows/
    echo   2. Adicione a pasta bin ao PATH, por exemplo:
    echo        C:\Program Files\PostgreSQL\16\bin
    echo      Painel de Controle ^> Sistema ^> Configuracoes avancadas ^>
    echo      Variaveis de Ambiente ^> Path ^> Novo.
    echo   3. Feche e abra o terminal novamente e rode este script de novo.
    goto :falha
)

rem --- servidor acessivel? ----------------------------------------------
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d postgres -w -tAc "SELECT 1" >nul 2>"%TEMP%\fisio_psql_erro.txt"
if errorlevel 1 (
    echo [ERRO] Nao foi possivel conectar ao PostgreSQL em %DB_HOST%:%DB_PORT% com o usuario %DB_USER%.
    echo.
    echo  Detalhe:
    type "%TEMP%\fisio_psql_erro.txt"
    echo.
    echo  Verifique se o servico do PostgreSQL esta iniciado ^(services.msc^)
    echo  e se a senha do usuario %DB_USER% esta correta.
    echo  Para usar outra senha: set FISIO_DB_PASSWORD=sua_senha  e rode o script novamente.
    goto :falha
)
echo [OK] PostgreSQL acessivel em %DB_HOST%:%DB_PORT%.

rem --- o banco ja existe? -----------------------------------------------
set "EXISTE="
for /f "usebackq delims=" %%R in (`psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d postgres -w -tAc "SELECT 1 FROM pg_database WHERE datname = '%DB_NAME%'"`) do set "EXISTE=%%R"

if "%EXISTE%"=="1" (
    echo [OK] O banco "%DB_NAME%" ja existe. Nada foi alterado.
    goto :sucesso
)

rem --- cria apenas se nao existir ---------------------------------------
echo Criando o banco "%DB_NAME%"...
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d postgres -w -v ON_ERROR_STOP=1 -c "CREATE DATABASE %DB_NAME% ENCODING 'UTF8'" >nul 2>"%TEMP%\fisio_psql_erro.txt"
if errorlevel 1 (
    echo [ERRO] Nao foi possivel criar o banco "%DB_NAME%".
    type "%TEMP%\fisio_psql_erro.txt"
    goto :falha
)
echo [OK] Banco "%DB_NAME%" criado com sucesso.

:sucesso
echo.
echo Proximo passo: execute iniciar-local.bat
echo ^(o Flyway cria as tabelas automaticamente na primeira inicializacao^).
if exist "%TEMP%\fisio_psql_erro.txt" del /q "%TEMP%\fisio_psql_erro.txt" >nul 2>&1
echo.
pause
endlocal
exit /b 0

:falha
if exist "%TEMP%\fisio_psql_erro.txt" del /q "%TEMP%\fisio_psql_erro.txt" >nul 2>&1
echo.
pause
endlocal
exit /b 1
