# Auditoria e Refatoracao

Data: 2026-03-06

## Escopo auditado

- Controllers
- Services
- Repositories
- Entidades JPA
- Migrations Flyway
- Templates Thymeleaf
- JavaScript e CSS
- Configuracao Spring Security

## Problemas criticos encontrados

### 1. Baixa rapida podia superpagar lancamentos parciais

`MovementService.quickPayPlanItem(...)` gerava `Movement` com o valor total previsto do `PlanItem`, mesmo quando ja existiam pagamentos parciais ou ajustes de desconto/juros.

Impacto:

- saldo restante ficava incorreto
- status podia ir para `PAGO` com valor realizado acima do necessario
- dashboard e relatorios herdavam distorcao financeira

Status:

- corrigido

### 2. Ownership de despesa paga estava ligado a `createdBy`, nao ao dono da conta

O modulo `ExpensePayment` usava `createdBy` como criterio de acesso e como filtro de consultas consolidadas. Se um administrador cadastrasse um pagamento em nome de outro usuario, o dono real da conta podia perder visibilidade do registro.

Impacto:

- inconsistencia entre conta, dashboard e despesas pagas
- risco funcional e de seguranca por escopo incorreto

Status:

- corrigido com priorizacao do `ownerUser` da conta e fallback para `createdBy`

### 3. Detalhe de despesa paga quebrava com auditoria parcial

`ExpensePaymentService.getDetail(...)` montava `Set.of(createdBy, updatedBy)` e falhava com `NullPointerException` quando `updatedBy` era nulo.

Impacto:

- tela de detalhe podia quebrar para registros legitimos

Status:

- corrigido

## Problemas importantes encontrados

### 4. Regra financeira duplicada em varios servicos

Calculo de:

- valor efetivo esperado
- valor restante
- status `PENDENTE` / `PARCIAL` / `PAGO`

estava repetido em `MovementService`, `PersonalFinanceMonthService` e `PersonalFinanceReportService`.

Risco:

- divergencia futura de regra de negocio
- manutencao lenta

Status:

- corrigido com utilitario centralizado `PlanItemFinanceUtils`

### 5. Historico de movimentos dependia de colecao lazy do `PlanItem`

O historico era montado a partir de `planItem.getMovements()` com ordenacao em memoria.

Risco:

- N+1 para contas
- maior custo de serializacao e montagem

Status:

- corrigido com consulta dedicada no repository

## Problemas relevantes mantidos como pendencia controlada

### 6. `PersonalFinanceController` esta monolitico

O controller principal concentra painel, cadastros, lancamentos, movimentos, relatorios, exports e atalhos legados.

Risco:

- baixa coesao
- manutencao cara
- maior chance de regressao transversal

Acao recomendada:

- extrair por contexto: `DashboardController`, `CatalogController`, `PlanItemController`, `ReportController`

### 7. `ExpensePaymentService.listPayments(...)` filtra em memoria

Atualmente a listagem carrega tudo com `findAllWithDetails()` e aplica filtro/ordenacao em Java.

Risco:

- escalabilidade ruim
- custo desnecessario no banco e na JVM

Acao recomendada:

- migrar para consulta paginada com filtros no repository

### 8. `app.security.test-admin.enabled` esta habilitado por padrao

Configuracao atual em `application.properties` mantem login de teste habilitado por default.

Risco:

- exposicao indevida caso a aplicacao suba fora do ambiente controlado

Acao recomendada:

- desabilitar por padrao e ativar apenas por variavel de ambiente em desenvolvimento

### 9. `ModelAttributes` executa notificacoes em toda requisicao autenticada

O `ControllerAdvice` calcula notificacoes globais e pode disparar leituras pesadas em cascata para cada pagina financeira.

Risco:

- custo repetido por request
- degradacao perceptivel em paginas com mais agregacoes

Acao recomendada:

- introduzir cache por request ou bundle de contexto financeiro

### 10. Existem rotas/artefatos legados e ambiguidade de navegacao

Exemplos observados:

- aliases antigos em `/financeiro-pessoal/lancamento*`
- template `financeiro-pessoal/despesas/pagamentos/list.html` sem renderizacao direta pelo controller atual
- diretorios operacionais `target/` e `tmp/` no workspace

Acao recomendada:

- remover aliases apos telemetry/manual validation
- decidir se o modulo de despesas pagas tera listagem propria ou apenas redirecionamento

## Refatoracoes aplicadas

- Centralizacao da logica financeira em `PlanItemFinanceUtils`
- Correcao da baixa rapida para pagar apenas o saldo restante
- Consulta dedicada para historico de movimentos
- Ajuste de ownership e escopo de `ExpensePayment`
- Correcao de `NullPointerException` na auditoria de detalhe
- Inclusao de testes de regressao para bugs corrigidos
- Inclusao de `.gitignore` para artefatos de build e temporarios

## Validacao executada

- `mvnw.cmd test`
- `mvnw.cmd spotless:apply`
- `mvnw.cmd test` novamente apos formatacao

Resultado:

- 45 testes passando

## Observacao final

Nao removi endpoints legados, templates ambigueos nem alterei nomes de tabelas para preservar compatibilidade com o banco e com a navegacao atual. As pendencias restantes exigem refatoracao estrutural maior ou validacao funcional com uso real do sistema.
