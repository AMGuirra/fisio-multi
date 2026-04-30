# Relatorio de Contexto do Sistema Financeiro Pessoal

Gerado em: 2026-03-10
Workspace: `D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal`
Objetivo: consolidar um retrato tecnico e funcional do sistema para repasse a outro ChatGPT, outra IA ou outro desenvolvedor.

## 1. Resumo executivo

- O projeto atual e um sistema web local de controle financeiro pessoal.
- A arquitetura e server-side renderizada com Spring Boot + Thymeleaf + Bootstrap.
- O sistema cobre autenticacao, painel financeiro, planejamento mensal, baixas e movimentos, despesas avulsas, cartoes de credito, transferencias entre contas, metas financeiras, calendario, BI financeiro e relatorios com exportacao.
- O backend esta organizado em 9 controllers, 21 services, 18 entidades JPA, 18 repositories, 21 migrations Flyway e 54 templates HTML.
- A base de dados esperada e PostgreSQL, com `spring.jpa.hibernate.ddl-auto=validate` e schema governado por Flyway.
- Em 2026-03-10 foi executado `.\mvnw.cmd test` com sucesso: 79 testes passando, 0 falhas, 0 erros.
- O worktree Git esta limpo no branch `main`, mas o historico visivel do repositorio exposto aqui tem apenas 1 commit. Por isso, a evolucao funcional esta melhor representada pelas migrations e pelos documentos em `docs/`.

## 2. Stack real em uso

- Java 25
- Spring Boot 3.5.11
- Spring Web
- Spring Data JPA
- Spring Security
- Spring Validation
- Spring Mail
- Thymeleaf
- Thymeleaf Layout Dialect
- Thymeleaf Extras Spring Security
- PostgreSQL
- Flyway
- Apache POI para Excel
- OpenPDF para PDF
- Lombok
- Maven Wrapper

Observacao importante:

- O `README.md` ainda menciona Spring Boot 3.4, mas o `pom.xml` atual usa Spring Boot 3.5.11.

## 3. Configuracao e execucao

- Porta padrao: `8082`
- Locale fixo: `pt_BR`
- Datas formatadas no padrao brasileiro
- Banco padrao: `jdbc:postgresql://localhost:5432/financeiro`
- SMTP padrao local: `localhost:1025`
- URL base padrao: `http://localhost:8082`
- Flyway: habilitado, com `baseline-on-migrate=true` e `validate-on-migrate=true`
- JPA: `ddl-auto=validate`

Variaveis relevantes:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `APP_BASE_URL`
- `APP_SECURITY_BOOTSTRAP_PASSWORDS_ENABLED`
- `APP_SECURITY_TEST_ADMIN_ENABLED`
- `APP_SECURITY_TEST_ADMIN_LOGIN`
- `APP_SECURITY_TEST_ADMIN_PASSWORD`

Formas de subir:

- Desenvolvimento: `.\mvnw.cmd spring-boot:run`
- JAR ja empacotado: `startup-serv.bat`

## 4. Modulos implementados

### 4.1 Acesso, seguranca e conta

- Login com Spring Security
- Logout
- Esqueci minha senha por e-mail
- Reset de senha por token com expiracao de 1 hora
- Alteracao da propria senha
- Sugestao de senha forte
- Perfis de usuario
- Controle por role e por ownership
- Tema por usuario
- Registro de ultimo acesso

### 4.2 Painel principal

- Painel financeiro mensal
- Saldo atual
- Valores recebidos no mes
- Valores pagos no mes
- Valores a pagar e a receber
- Saldo projetado
- Resultado realizado e projetado
- Top categorias de despesa
- Proximas contas
- Saldos por conta
- Metas em andamento
- Atalhos rapidos
- Grafico de saldo diario
- Tendencia de saude financeira

### 4.3 Analytics complementar

- Projecao financeira
- Analise mensal por categorias
- Comparacao com mes anterior
- Calendario financeiro do mes
- Snapshot de BI com medias e tendencia
- Notificacoes operacionais
- Insights financeiros gerados a partir dos agregados

### 4.4 Cadastros administrativos

- Usuarios
- Grupos de categoria
- Subgrupos de categoria
- Contas financeiras
- Cartoes de credito

### 4.5 Planejamento mensal

- Cadastro e edicao de lancamentos previstos
- Vinculo por grupo, subgrupo e usuario responsavel
- Tipos de vencimento:
- `DAY_OF_MONTH`
- `SPECIFIC_DATE`
- `CREDIT_CARD_INVOICE`
- `VARIABLE`
- Resumo mensal
- Totais por grupo
- Historico de pagamentos
- Exportacao da lista de lancamentos em PDF e Excel

### 4.6 Templates e automacao interna do planejamento

- Templates parcelados
- Templates recorrentes
- Geracao automatica de itens futuros
- Inativacao de template
- Encerramento de template
- Quitacao antecipada de parcelamentos
- Registro de lote de quitacao antecipada

### 4.7 Movimentos e baixas

- Registro manual de baixa
- Edicao de baixa
- Cancelamento de baixa
- Baixa rapida
- Historico por lancamento
- Filtros por tipo, status, conta, usuario e busca textual
- Tratamento de diferenca de valor:
- `STANDARD`
- `PARTIAL`
- `DISCOUNT`
- `INTEREST`

### 4.8 Transferencias entre contas

- Cadastro de transferencia
- Historico de transferencias
- Detalhe de transferencia
- Exclusao de transferencia
- Geracao de movimentos associados

### 4.9 Cartoes de credito

- Cadastro de cartao com fechamento e vencimento
- Compra a vista ou parcelada
- Geracao de parcelas
- Geracao e atualizacao de fatura
- Sincronizacao da fatura com `PlanItem`
- Pagamento de fatura com baixa financeira
- Bloqueio de edicao/exclusao quando ja houver parcela paga

### 4.10 Despesas avulsas

- Cadastro de pagamento avulso
- Edicao
- Cancelamento
- Exclusao
- Competencia mensal
- Favorecido
- Comprovante por nome e URL
- Formas de pagamento:
- `PIX`
- `DINHEIRO`
- `CARTAO_CREDITO`
- `CARTAO_DEBITO`
- `BOLETO`
- `TRANSFERENCIA`
- `OUTRO`

### 4.11 Metas e planejamento financeiro

- Metas de reserva (`SAVINGS`)
- Metas de limite por categoria (`CATEGORY_LIMIT`)
- Tela de metas
- Tela de planejamento
- Acompanhamento de progresso
- Alertas quando limite esta perto ou estourado

### 4.12 Relatorios e exportacoes

- Relatorio mensal
- Relatorio por periodo
- Relatorio de fluxo de caixa
- Relatorio por categorias
- Exportacao PDF
- Exportacao Excel
- Exportacao CSV para relatorio por periodo

## 5. Rotas e telas principais

Rotas mais importantes por modulo:

- `/login`
- `/esqueci-senha`
- `/reset-senha`
- `/minha-senha`
- `/tema`
- `/financeiro-pessoal`
- `/financeiro-pessoal/calendario`
- `/financeiro-pessoal/metas`
- `/financeiro-pessoal/planejamento`
- `/financeiro-pessoal/cadastros/usuarios`
- `/financeiro-pessoal/cadastros/grupos`
- `/financeiro-pessoal/cadastros/tipos`
- `/financeiro-pessoal/cadastros/contas`
- `/financeiro-pessoal/cadastros/cartoes`
- `/financeiro-pessoal/lancamentos`
- `/financeiro-pessoal/lancamentos/novo`
- `/financeiro-pessoal/lancamentos/totais-grupo`
- `/financeiro-pessoal/lancamentos/historico`
- `/financeiro-pessoal/movimentos`
- `/financeiro-pessoal/movimentos/receitas`
- `/financeiro-pessoal/movimentos/despesas`
- `/financeiro-pessoal/movimentos/contas-a-pagar`
- `/financeiro-pessoal/movimentos/contas-pagas`
- `/financeiro-pessoal/movimentos/contas-a-receber`
- `/financeiro-pessoal/movimentos/contas-recebidas`
- `/financeiro-pessoal/transferencias`
- `/financeiro-pessoal/transferencias/nova`
- `/financeiro-pessoal/cartoes`
- `/financeiro-pessoal/despesas/pagamentos`
- `/financeiro-pessoal/relatorios/mensal`
- `/financeiro-pessoal/relatorios/periodo`
- `/financeiro-pessoal/relatorios/fluxo`
- `/financeiro-pessoal/relatorios/categorias`
- `/financeiro-pessoal/relatorios/bi`

Observacao:

- Ha 119 anotacoes de mapeamento (`@RequestMapping`, `@GetMapping`, `@PostMapping`) somando controllers e rotas.
- Existem aliases legados em `/financeiro-pessoal/lancamento*` que redirecionam para `/financeiro-pessoal/lancamentos*`.

## 6. Modelo de dados atual

Entidades principais observadas no codigo:

- `Usuario`
- `Person`
- `Account`
- `CategoryGroup`
- `CategorySubgroup`
- `PlanItem`
- `PlanTemplate`
- `Movement`
- `MovementCancellation`
- `AccountTransfer`
- `ExpensePayment`
- `CreditCard`
- `CreditCardPurchase`
- `CreditCardInstallment`
- `CreditCardStatement`
- `FinancialGoal`
- `FinancialAuditLog`
- `EarlySettlementBatch`

Relacoes de negocio mais importantes:

- Usuario possui tema, credenciais, token de reset e perfil
- Conta pertence a um usuario responsavel
- Cartao pertence a um usuario responsavel e pode apontar conta padrao
- PlanItem pertence a grupo, subgrupo, usuario e opcionalmente cartao e template
- Movement referencia um `PlanItem` ou uma `AccountTransfer`
- `CreditCardStatement` sincroniza com `PlanItem` para aparecer no financeiro geral
- `ExpensePayment` usa `CategorySubgroup` e `Account`
- `FinancialGoal` pode ser por economia geral ou limite de categoria
- `FinancialAuditLog` registra eventos relevantes

## 7. Linha do tempo funcional pelas migrations

- `V1`: estrutura inicial do financeiro pessoal, usuarios e base do dominio
- `V2`: tipos de categoria e defaults
- `V3`: ownership por usuario e cadastro vinculado
- `V4`: bonus em categorias
- `V5`: deduplicacao de grupos e tipos
- `V6`: indices de performance
- `V7`: templates parcelados e recorrentes
- `V8`: modulo de pagamentos avulsos de despesa
- `V9`: tratamento de diferenca em movimentos
- `V10`: modulo de transferencias entre contas
- `V11`: modulo de metas financeiras
- `V12`: suporte a data especifica de vencimento
- `V13` a `V19`: evolucao forte do modulo de cartoes e alinhamento de schema
- `V20`: reset controlado do catalogo bootstrap de categorias
- `V21`: endurecimento do motor de lancamentos e templates

## 8. Qualidade, testes e maturidade

Estado atual validado em 2026-03-10:

- `.\mvnw.cmd test` executado com sucesso
- 79 testes passando
- 0 falhas
- 0 erros
- 0 testes ignorados

Classes de teste presentes:

- controllers
- forms
- security
- services
- DTO/view helper
- utilitarios monetarios

Cobertura funcional claramente testada:

- cartoes
- movimentos
- pagamentos avulsos
- relatorios
- templates
- usuarios
- dashboard financeiro
- transferencias

Observacoes tecnicas do teste:

- O build passou, mas houve warnings de compatibilidade futura ligados a Mockito agent e bibliotecas da stack Java. Nao houve impacto no resultado atual.

## 9. Auditoria e pontos de atencao atuais

O documento `docs/AUDITORIA_REFATORACAO_2026-03-06.md` registra correcoes recentes importantes:

- baixa rapida corrigida para nao superpagar itens parciais
- ownership de despesa avulsa corrigido para priorizar o dono da conta
- correcao de `NullPointerException` em detalhe de despesa
- centralizacao da regra financeira em `PlanItemFinanceUtils`
- historico de movimentos movido para consulta dedicada

Pendencias ou riscos ainda abertos no proprio repositorio:

- `PersonalFinanceController` esta muito grande e mistura muitos contextos
- `ExpensePaymentService.listPayments(...)` ainda filtra em memoria
- `app.security.test-admin.enabled` esta `true` por padrao em `application.properties`
- `ModelAttributes` monta notificacoes a cada request autenticada em `/financeiro-pessoal`
- existem rotas e artefatos legados que ainda convivem com a estrutura nova

## 10. Artefatos auxiliares uteis

Documentos relevantes para o sistema atual:

- `README.md`
- `docs/ROTEIRO_FUNCIONALIDADES_SISTEMA.md`
- `docs/README_OPERACIONAL_V18.md`
- `docs/AUDITORIA_REFATORACAO_2026-03-06.md`
- `docs/sql/seed_demo_v18.sql`
- `docs/sql/seed_hml_v18.sql`
- `docs/sql/seed_treinamento_v18.sql`

Artefatos que parecem legado ou contexto externo ao financeiro pessoal atual:

- `docs/ENTREGA-A-MODELO-ENTIDADES.md` fala de um modulo operacional legado
- `docs/checklists/checklist_pos_v18.md` tambem esta voltado a empresa, cliente e contratos de uma versao anterior
- diretorios `target/` e `tmp/` existem no workspace e contem build/logs/renderizacoes temporarias

Conclusao pratica:

- Para conversar com outro ChatGPT sobre o sistema financeiro pessoal, use como fonte principal o codigo atual, as migrations, o roteiro funcional e a auditoria.
- Nao use o documento de locadora como contexto do sistema atual.

## 11. Inventario estrutural rapido

- Branch atual: `main`
- Commits visiveis: `1`
- Controllers: `9`
- Services: `21`
- Entidades JPA: `18`
- Repositories: `18`
- Migrations Flyway: `21`
- Templates HTML: `54`
- Arquivos JS proprios: `11`
- Arquivos CSS proprios: `9`
- Classes de teste: `19`

## 12. Texto pronto para colar no ChatGPT

Use o texto abaixo como contexto inicial:

```text
Estou trabalhando em um sistema chamado "Financeiro Pessoal". Considere este contexto como fonte primaria:

- E um sistema web local de controle financeiro pessoal feito em Java 25, Spring Boot 3.5.11, Spring MVC, Spring Security, JPA, PostgreSQL, Flyway, Thymeleaf e Bootstrap.
- O sistema roda por padrao na porta 8082.
- O dominio principal inclui: usuarios, contas, grupos e subgrupos de categoria, lancamentos planejados (`PlanItem`), templates recorrentes e parcelados (`PlanTemplate`), movimentos/baixas (`Movement`), cancelamentos de movimento, transferencias entre contas, cartoes de credito, compras de cartao, parcelas, faturas, pagamentos avulsos de despesa, metas financeiras, auditoria financeira e quitacao antecipada.
- Existe autenticacao com login, troca de tema, troca de senha, recuperacao de senha por token e controle por role + ownership.
- O sistema possui painel principal, calendario financeiro, BI financeiro, metas, planejamento, cadastros administrativos, lancamentos, central de movimentos, transferencias, cartoes, despesas avulsas e relatorios.
- O planejamento suporta vencimento por dia do mes, data especifica, fatura de cartao e vencimento variavel.
- O motor financeiro trata pagamentos parciais, desconto, juros/acrescimo e baixa rapida.
- O modulo de cartoes sincroniza faturas com `PlanItem` para que a fatura apareca no financeiro geral.
- Existem relatorios mensal, por periodo, fluxo de caixa e performance por categoria, com exportacoes PDF/Excel e CSV no relatorio por periodo.
- Ha modulo de metas com dois tipos principais: reserva/economia e limite por categoria.
- Os testes automatizados foram executados em 2026-03-10 e passaram: 79 testes, 0 falhas.
- As principais pendencias tecnicas atuais sao: controller principal muito grande, filtro de despesas avulsas ainda em memoria, login de admin de teste habilitado por padrao e notificacoes montadas em toda request autenticada.
- Ignore documentos de modulo operacional legado presentes no repositorio, porque sao artefatos antigos e nao descrevem este sistema.

Quero sua ajuda para: [descreva aqui o que voce quer que o ChatGPT faca].

Quando responder, nao invente modulos fora desse contexto. Se faltar algum detalhe importante, me diga exatamente qual arquivo ou area do sistema voce quer aprofundar.
```

## 13. Leitura recomendada para aprofundar

Se outra IA precisar mergulhar mais no codigo, os melhores pontos de entrada sao:

- `src/main/java/com/controle/finpessoal/controller/PersonalFinanceController.java`
- `src/main/java/com/controle/finpessoal/service/PersonalFinanceMonthService.java`
- `src/main/java/com/controle/finpessoal/service/MovementService.java`
- `src/main/java/com/controle/finpessoal/service/CreditCardService.java`
- `src/main/java/com/controle/finpessoal/service/ExpensePaymentService.java`
- `src/main/java/com/controle/finpessoal/service/DashboardService.java`
- `src/main/java/com/controle/finpessoal/service/DashboardAnalyticsService.java`
- `src/main/java/com/controle/finpessoal/service/PersonalFinanceReportService.java`
- `src/main/resources/db/migration/`
- `src/main/resources/templates/financeiro-pessoal/`
