# Relatório de Execução - Comentários e UX

## Resumo

- Compilação validada com `./mvnw.cmd -q -DskipTests compile`.
- Arquivos alterados em `src/`: 191.
- Escopo: comentários em Java/HTML e melhorias de UX nas telas Thymeleaf principais.

## Arquivos Alterados por Categoria

### Entities / Models (17)
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\domain\Account.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\domain\AccountTransfer.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\domain\CategoryGroup.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\domain\CreditCard.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\domain\CreditCardInstallment.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\domain\CreditCardPurchase.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\domain\CreditCardStatement.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\domain\EarlySettlementBatch.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\domain\ExpensePayment.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\domain\FinancialAuditLog.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\domain\FinancialGoal.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\domain\Movement.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\domain\MovementCancellation.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\domain\PlanItem.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\domain\PlanTemplate.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\domain\Usuario.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\domain\base\Auditavel.java

### Repositories (16)
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\repository\AccountRepository.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\repository\AccountTransferRepository.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\repository\CategoryGroupRepository.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\repository\CreditCardInstallmentRepository.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\repository\CreditCardPurchaseRepository.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\repository\CreditCardRepository.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\repository\CreditCardStatementRepository.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\repository\EarlySettlementBatchRepository.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\repository\ExpensePaymentRepository.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\repository\FinancialAuditLogRepository.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\repository\FinancialGoalRepository.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\repository\MovementCancellationRepository.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\repository\MovementRepository.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\repository\PlanItemRepository.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\repository\PlanTemplateRepository.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\repository\UsuarioRepository.java

### Services (20)
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\service\AccountTransferService.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\service\CategoryAnalyticsService.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\service\CreditCardService.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\service\DashboardAnalyticsService.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\service\DashboardService.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\service\EmailService.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\service\ExpensePaymentService.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\service\FinanceiroDashboardService.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\service\FinancialAuditService.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\service\FinancialGoalService.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\service\FinancialInsightService.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\service\FinancialProjectionService.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\service\GoalService.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\service\MovementService.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\service\NotificationService.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\service\PersonalFinanceMonthService.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\service\PersonalFinanceReportExportService.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\service\PersonalFinanceReportService.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\service\PlanTemplateService.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\service\UsuarioService.java

### Controllers (9)
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\controller\AccountTransferController.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\controller\AuthController.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\controller\ContaAcessoController.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\controller\CreditCardController.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\controller\ExpensePaymentController.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\controller\FinancialGoalController.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\controller\HomeController.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\controller\MovementController.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\controller\PersonalFinanceController.java

### DTOs / Requests / Responses / Enums (57)
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\controller\form\CardPurchaseForm.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\controller\form\EarlySettlementForm.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\controller\form\ExpensePaymentForm.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\controller\form\FinancialGoalForm.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\controller\form\MovementForm.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\controller\form\PersonalFinanceAccountForm.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\controller\form\PersonalFinanceCategoryGroupForm.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\controller\form\PersonalFinanceCreditCardForm.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\controller\form\PersonalFinanceInstallmentTemplateForm.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\controller\form\PersonalFinancePaymentForm.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\controller\form\PersonalFinancePlanItemForm.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\controller\form\PersonalFinanceRecurringTemplateForm.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\controller\form\PersonalFinanceUserForm.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\controller\form\TransferCreateForm.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\domain\enums\AccountType.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\domain\enums\AuditActionType.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\domain\enums\AuditEntityType.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\domain\enums\CardInvoiceStatus.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\domain\enums\CardPurchaseType.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\domain\enums\ExpensePaymentMethod.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\domain\enums\ExpensePaymentStatus.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\domain\enums\FinancialClassification.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\domain\enums\FinancialGoalType.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\domain\enums\MovementDifferenceHandling.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\domain\enums\MovementStatus.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\domain\enums\MovementType.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\domain\enums\PlanItemDueType.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\domain\enums\PlanItemOriginType.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\domain\enums\PlanItemStatus.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\domain\enums\PlanItemUpdateScope.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\domain\enums\PlanTemplateType.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\domain\enums\RecurrenceFrequency.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\domain\enums\TipoUsuario.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\service\dto\analytics\CategoryAnalysisDTO.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\service\dto\analytics\DashboardMetricsDTO.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\service\dto\analytics\FinanceiroDashboardView.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\service\dto\analytics\FinancialInsightDTO.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\service\dto\analytics\FinancialProjectionDTO.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\service\dto\analytics\MonthlyComparisonDTO.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\service\dto\dashboard\DashboardView.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\service\dto\personalfinance\AccountPaymentSummaryView.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\service\dto\personalfinance\CashFlowPointView.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\service\dto\personalfinance\CashFlowReportView.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\service\dto\personalfinance\CategoryPerformanceReportView.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\service\dto\personalfinance\CategoryPerformanceRowView.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\service\dto\personalfinance\MonthlyGroupSummaryView.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\service\dto\personalfinance\MonthlyItemView.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\service\dto\personalfinance\MonthlyMovementView.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\service\dto\personalfinance\MonthlyPlanView.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\service\dto\personalfinance\MonthlyReportView.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\service\dto\personalfinance\MonthlyStatusSummaryView.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\service\dto\personalfinance\MonthlyTotalsView.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\service\dto\personalfinance\PeriodGroupSummaryView.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\service\dto\personalfinance\PeriodMonthlySummaryView.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\service\dto\personalfinance\PeriodReportView.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\service\dto\usermanagement\UserManagementItemView.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\service\dto\usermanagement\UserManagementPageView.java

### Config / Security / Util (17)
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\FinanceiroPessoalApplication.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\config\AppErrorAttributes.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\config\ModelAttributes.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\config\PasswordConfig.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\config\SecurityConfig.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\config\StartupSecurityInitializer.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\config\TrailingSlashRedirectFilter.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\exception\GlobalExceptionHandler.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\security\AppUserDetails.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\security\AppUserDetailsService.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\security\CurrentUserService.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\security\UsuarioLoginSuccessListener.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\util\MoneyUtils.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\util\PermissionValidator.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\util\PlanItemFinanceUtils.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\util\ValidationUtils.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\application.properties

### Templates Thymeleaf (53)
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\auth\change-password.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\auth\forgot-password.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\auth\login.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\auth\reset-password.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\dashboard\calendar.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\error.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\cadastros-cartoes.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\cadastros-contas.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\cadastros-grupos.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\cadastros-usuarios.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\cartoes\detalhes.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\despesas\pagamentos\detail.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\despesas\pagamentos\form.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\despesas\pagamentos\list.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\lancamento-historico.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\lancamento-itens.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\lancamento-novo-item.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\lancamento-totais.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\movimento-form.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\movimento-historico.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\movimentos-tipo.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\movimentos.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\painel.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\relatorios-categorias.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\relatorios-fluxo.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\relatorios-periodo.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\transferencias\detail.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\transferencias\form.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\transferencias\list.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\fragments\action-buttons.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\fragments\alert-card.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\fragments\app-ui.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\fragments\atalhos-acoes.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\fragments\chart-card.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\fragments\data-table.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\fragments\expense-payment-actions.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\fragments\filter-bar.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\fragments\header.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\fragments\layout.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\fragments\metric-card.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\fragments\month-selector.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\fragments\movimentos-filter-fields.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\fragments\navbar.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\fragments\page-header.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\fragments\payment-difference-fields.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\fragments\redesign.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\fragments\report-filter-card.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\fragments\report-flow.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\fragments\search-bar.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\layout.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\metas\index.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\planejamento\index.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\relatorios\bi-financeiro.html

### CSS / JS (2)
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\static\css\app-theme.css
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\static\js\pf-ui-utils.js

## Melhorias de UX por Tela

- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\painel.html -> 8 elementos de orientação/UX
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\planejamento\index.html -> 11 elementos de orientação/UX
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\movimentos.html -> 13 elementos de orientação/UX
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\cadastros-contas.html -> 9 elementos de orientação/UX
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\cadastros-grupos.html -> 8 elementos de orientação/UX
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\cadastros-cartoes.html -> 14 elementos de orientação/UX
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\movimento-form.html -> 6 elementos de orientação/UX
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\lancamento-novo-item.html -> 13 elementos de orientação/UX
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\despesas\pagamentos\form.html -> 10 elementos de orientação/UX
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\despesas\pagamentos\list.html -> 4 elementos de orientação/UX
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\relatorios-fluxo.html -> 2 elementos de orientação/UX
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\relatorios-categorias.html -> 2 elementos de orientação/UX
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\relatorios-periodo.html -> 2 elementos de orientação/UX

## Trechos Modificados de UX

### D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\painel.html
```
<nav class="pf-breadcrumb" aria-label="Breadcrumb">
<a class="pf-breadcrumb__link" th:href="@{/financeiro-pessoal(mes=${selectedMonthValue})}">Dashboard</a>
<span class="pf-breadcrumb__separator">›</span>
<span class="pf-breadcrumb__current">Painel do mês</span>
<div class="pf-tip-card" data-pf-tip-card>
<div class="pf-tip-card__header">
<span class="pf-tip-card__title">
<button type="button" class="pf-tip-card__close" data-pf-tip-close aria-label="Fechar dica">
```

### D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\planejamento\index.html
```
<nav class="pf-breadcrumb" aria-label="Breadcrumb">
<a class="pf-breadcrumb__link" th:href="@{/financeiro-pessoal(mes=${selectedMonthValue})}">Dashboard</a>
<span class="pf-breadcrumb__separator">›</span>
<span class="pf-breadcrumb__current">Planejamento</span>
<div class="pf-tip-card" data-pf-tip-card>
<div class="pf-tip-card__header">
<span class="pf-tip-card__title">
<button type="button" class="pf-tip-card__close" data-pf-tip-close aria-label="Fechar dica">
```

### D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\movimentos.html
```
<nav class="pf-breadcrumb" aria-label="Breadcrumb">
<a class="pf-breadcrumb__link" th:href="@{/financeiro-pessoal(mes=${selectedMonthValue})}">Dashboard</a>
<span class="pf-breadcrumb__separator">›</span>
<span class="pf-breadcrumb__current">Movimentos</span>
<div class="pf-tip-card" data-pf-tip-card>
<div class="pf-tip-card__header">
<span class="pf-tip-card__title">
<button type="button" class="pf-tip-card__close" data-pf-tip-close aria-label="Fechar dica">
```

### D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\cadastros-contas.html
```
<nav class="pf-breadcrumb" aria-label="Breadcrumb">
<a class="pf-breadcrumb__link" th:href="@{/financeiro-pessoal(mes=${selectedMonthValue})}">Dashboard</a>
<span class="pf-breadcrumb__separator">›</span>
<a class="pf-breadcrumb__link" th:href="@{/financeiro-pessoal/cadastros/usuarios(mes=${selectedMonthValue})}">Cadastros</a>
<span class="pf-breadcrumb__separator">›</span>
<span class="pf-breadcrumb__current">Contas</span>
<div class="pf-tip-card" data-pf-tip-card>
<div class="pf-tip-card__header">
```

### D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\cadastros-grupos.html
```
<nav class="pf-breadcrumb" aria-label="Breadcrumb">
<a class="pf-breadcrumb__link" th:href="@{/financeiro-pessoal(mes=${selectedMonthValue})}">Dashboard</a>
<span class="pf-breadcrumb__separator">›</span>
<a class="pf-breadcrumb__link" th:href="@{/financeiro-pessoal/cadastros/usuarios(mes=${selectedMonthValue})}">Cadastros</a>
<span class="pf-breadcrumb__separator">›</span>
<span class="pf-breadcrumb__current" th:text="${isSubclassificationCatalog ? 'Subclassificações' : 'Classificações'}">Classificações</span>
<div class="pf-tip-card" data-pf-tip-card>
<div class="pf-tip-card__header">
```

### D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\cadastros-cartoes.html
```
<nav class="pf-breadcrumb" aria-label="Breadcrumb">
<a class="pf-breadcrumb__link" th:href="@{/financeiro-pessoal(mes=${selectedMonthValue})}">Dashboard</a>
<span class="pf-breadcrumb__separator">›</span>
<a class="pf-breadcrumb__link" th:href="@{/financeiro-pessoal/cadastros/usuarios(mes=${selectedMonthValue})}">Cadastros</a>
<span class="pf-breadcrumb__separator">›</span>
<span class="pf-breadcrumb__current">Cartões</span>
<div class="pf-tip-card" data-pf-tip-card>
<div class="pf-tip-card__header">
```

### D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\movimento-form.html
```
<nav class="pf-breadcrumb" aria-label="Breadcrumb">
<a class="pf-breadcrumb__link" th:href="@{/financeiro-pessoal(mes=${selectedMonthValue})}">Dashboard</a>
<span class="pf-breadcrumb__separator">›</span>
<a class="pf-breadcrumb__link" th:href="@{/financeiro-pessoal/movimentos(mes=${selectedMonthValue})}">Movimentos</a>
<span class="pf-breadcrumb__separator">›</span>
<span class="pf-breadcrumb__current" th:text="${movementPageTitle}">Registrar movimento</span>
<div class="pf-tip-card" data-pf-tip-card>
<div class="pf-tip-card__header">
```

### D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\lancamento-novo-item.html
```
<nav class="pf-breadcrumb" aria-label="Breadcrumb">
<a class="pf-breadcrumb__link" th:href="@{/financeiro-pessoal(mes=${selectedMonthValue})}">Dashboard</a>
<span class="pf-breadcrumb__separator">›</span>
<a class="pf-breadcrumb__link" th:href="@{/financeiro-pessoal/movimentos(mes=${selectedMonthValue})}">Movimentos</a>
<span class="pf-breadcrumb__separator">›</span>
<span class="pf-breadcrumb__current">Novo lançamento</span>
<div class="pf-tip-card" data-pf-tip-card>
<div class="pf-tip-card__header">
```

### D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\despesas\pagamentos\form.html
```
<nav class="pf-breadcrumb" aria-label="Breadcrumb">
<a class="pf-breadcrumb__link" th:href="@{/financeiro-pessoal(mes=${selectedMonthValue})}">Dashboard</a>
<span class="pf-breadcrumb__separator">›</span>
<a class="pf-breadcrumb__link" th:href="@{/financeiro-pessoal/despesas/pagamentos(mes=${selectedMonthValue})}">Pagamentos</a>
<span class="pf-breadcrumb__separator">›</span>
<span class="pf-breadcrumb__current" th:text="${isNew} ? 'Novo pagamento' : 'Editar pagamento'">Pagamento</span>
<div class="pf-tip-card" data-pf-tip-card>
<div class="pf-tip-card__header">
```

### D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\despesas\pagamentos\list.html
```
<nav class="pf-breadcrumb" aria-label="Breadcrumb">
<a class="pf-breadcrumb__link" th:href="@{/financeiro-pessoal(mes=${selectedMonthValue})}">Dashboard</a>
<span class="pf-breadcrumb__separator">›</span>
<span class="pf-breadcrumb__current">Pagamentos avulsos</span>
<div class="pf-tip-card" data-pf-tip-card>
<div class="pf-tip-card__header">
<span class="pf-tip-card__title">
<button type="button" class="pf-tip-card__close" data-pf-tip-close aria-label="Fechar dica">
```

### D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\relatorios-fluxo.html
```
<nav class="pf-breadcrumb" aria-label="Breadcrumb">
<a class="pf-breadcrumb__link" th:href="@{/financeiro-pessoal(mes=${selectedMonthValue})}">Dashboard</a>
<span class="pf-breadcrumb__separator">›</span>
<a class="pf-breadcrumb__link" th:href="@{/financeiro-pessoal/relatorios/fluxo(mes=${selectedMonthValue})}">Relatórios</a>
<span class="pf-breadcrumb__separator">›</span>
<span class="pf-breadcrumb__current">Fluxo de caixa</span>
<div class="pf-tip-card" data-pf-tip-card>
<div class="pf-tip-card__header">
```

### D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\relatorios-categorias.html
```
<nav class="pf-breadcrumb" aria-label="Breadcrumb">
<a class="pf-breadcrumb__link" th:href="@{/financeiro-pessoal(mes=${selectedMonthValue})}">Dashboard</a>
<span class="pf-breadcrumb__separator">›</span>
<a class="pf-breadcrumb__link" th:href="@{/financeiro-pessoal/relatorios/categorias(mes=${selectedMonthValue})}">Relatórios</a>
<span class="pf-breadcrumb__separator">›</span>
<span class="pf-breadcrumb__current">Classificações</span>
<div class="pf-tip-card" data-pf-tip-card>
<div class="pf-tip-card__header">
```

### D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\relatorios-periodo.html
```
<nav class="pf-breadcrumb" aria-label="Breadcrumb">
<a class="pf-breadcrumb__link" th:href="@{/financeiro-pessoal(mes=${selectedMonthValue})}">Dashboard</a>
<span class="pf-breadcrumb__separator">›</span>
<a class="pf-breadcrumb__link" th:href="@{/financeiro-pessoal/relatorios/periodo(mes=${selectedMonthValue})}">Relatórios</a>
<span class="pf-breadcrumb__separator">›</span>
<span class="pf-breadcrumb__current">Comparativo por período</span>
<div class="pf-tip-card" data-pf-tip-card>
<div class="pf-tip-card__header">
```

### D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\fragments\layout.html
```
<div class="pf-toast-stack" aria-live="polite" aria-atomic="true">
<div th:if="${successMessage}" class="pf-toast pf-toast--success" data-pf-toast role="status">
<span class="pf-toast__icon" aria-hidden="true">
<div class="pf-toast__content">
<button type="button" class="btn-close" data-pf-toast-close aria-label="Fechar mensagem"></button>
<div th:if="${errorMessage}" class="pf-toast pf-toast--error" data-pf-toast role="alert">
<span class="pf-toast__icon" aria-hidden="true">
<div class="pf-toast__content">
```

### D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\static\css\app-theme.css
```
.pf-breadcrumb {
.pf-breadcrumb__link {
.pf-breadcrumb__link:hover,
.pf-breadcrumb__link:focus {
.pf-breadcrumb__separator {
.pf-breadcrumb__current {
.pf-tooltip-icon {
.pf-tooltip-icon:hover,
```

### D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\static\js\pf-ui-utils.js
```
toastElement.classList.add("pf-toast--closing");
function initToasts() {
var toasts = document.querySelectorAll("[data-pf-toast]");
var closeButton = toast.querySelector("[data-pf-toast-close]");
function initDismissibleTips() {
var card = button.closest("[data-pf-tip-card]");
ui.initToasts = initToasts;
ui.initDismissibleTips = initDismissibleTips;
```

