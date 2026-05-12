@echo off
chcp 65001 >nul
cd /d "%~dp0"

echo.
echo ========================================
echo  FisioGestao - Commit de Melhorias
echo ========================================
echo.

git add .
if %errorlevel% neq 0 (
    echo [ERRO] git add falhou.
    pause
    exit /b 1
)
echo [OK] git add concluido.

git commit -m "refactor: melhorias de performance, qualidade e corretude do sistema

PERFORMANCE:
- FinanceiroService: somarPorFiltro() agora usa COALESCE(SUM()) direto no banco
  via @Query, eliminando carregamento de todos os lancamentos em memoria
- DashboardService: listarAtivos().size() substituido por countByAtivoTrue(),
  query de contagem sem trazer registros para memoria
- LancamentoFinanceiroRepository: adicionado sumValorByTipoAndStatus() com @Query
- ClienteRepository: adicionado countByAtivoTrue()

TRANSACOES:
- AgendaService: @Transactional(readOnly=true) em 11 metodos de leitura
- FinanceiroService: @Transactional(readOnly=true) em 7 metodos de leitura
- PacienteService: @Transactional(readOnly=true) em 4 metodos de leitura

CONFIGURACAO:
- application.yml: thymeleaf.cache=true (producao); DevTools desativa em dev
- application.yml: timezone hardcoded substituido por dollar{app.timezone} em
  hibernate.jdbc.time_zone e jackson.time-zone (fonte unica de verdade)

INICIALIZACAO E SCHEMA:
- Criado SchemaCompatibilityValidator (ApplicationRunner): valida schema do banco
  na inicializacao em vez de revalidar via volatile flags a cada request
- PacienteService: adicionado marcarSchemaValidado() acionado pelo validator

EMAIL:
- AgendamentoEmailNotificationService: excecoes nao mais engolidas silenciosamente;
  BusinessException -> log.warn, excecoes inesperadas -> log.error com stack trace

QUALIDADE DE CODIGO:
- Removido @SuppressWarnings(null) de todos os 16 arquivos que o continham:
  AgendaService, FinanceiroService, PacienteService, ConfiguracaoClinicaService,
  BloqueioAgendaService, PasswordResetService, EmailConfiguracaoService,
  PacoteSessaoService, SolicitacaoAgendamentoService, AuditoriaController,
  InMemoryUserCatalog, UserMenuAccessService, DataInitializer,
  UserMenuAccessInterceptor, WebMvcConfig, SystemEmailService
- SessaoRepository: removido @SuppressWarnings(null) do override findById()

DOCUMENTACAO:
- ROTEIRO_FUNCIONALIDADES_SISTEMA.md reescrito: documento anterior descrevia
  sistema de financas pessoais sem relacao com o FisioGestao; novo conteudo
  documenta todos os modulos reais: agenda, pacientes, prontuario SOAP, planos
  de tratamento, portal do paciente, solicitacoes online, financeiro,
  relatorios, configuracoes e auditoria"

if %errorlevel% neq 0 (
    echo [ERRO] git commit falhou.
    pause
    exit /b 1
)
echo [OK] git commit concluido.

git push origin main
if %errorlevel% neq 0 (
    echo [ERRO] git push falhou. Verifique sua autenticacao no GitHub.
    pause
    exit /b 1
)

echo.
echo ========================================
echo  Push realizado com sucesso!
echo  https://github.com/AMGuirra/fisioterapia
echo ========================================
echo.
pause
