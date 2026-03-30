# Relatório da Correção de Português

## Arquivos alterados nesta varredura

- Templates HTML: 24
- Classes Java: 8
- Properties/YAML: 1
- JavaScript: 2
- Outros: 1
- Total: 36

## Caminhos completos

- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\README.md
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\application.properties
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\static\js\page-search.js
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\static\js\lancamentos-itens-actions.js
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\auth\login.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\error.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\dashboard\calendar.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\cadastros-cartoes.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\cadastros-contas.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\cadastros-grupos.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\despesas\pagamentos\form.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\despesas\pagamentos\list.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\financeiro-pessoal\lancamento-historico.html
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
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\fragments\navbar.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\metas\index.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\resources\templates\planejamento\index.html
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\config\ModelAttributes.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\controller\MovementController.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\controller\PersonalFinanceController.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\controller\form\FinancialGoalForm.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\service\FinanceiroDashboardService.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\service\GoalService.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\service\NotificationService.java
- D:\Desenvolvimento\1 projetos atuais\Financeiro Pessoal\src\main\java\com\controle\finpessoal\service\PersonalFinanceReportExportService.java

## Antes e depois (diff unificado)

Linhas removidas aparecem com `-` e linhas adicionadas aparecem com `+`.

```diff
diff --git a/README.md b/README.md
index d29c634..9381811 100644
--- a/README.md
+++ b/README.md
@@ -1 +1 @@
-﻿# Financeiro Pessoal
+# Financeiro Pessoal
@@ -14 +14 @@ Sistema local para controle financeiro pessoal com lançamento mensal e registro
-## Configuracao
+## Configuração
@@ -22 +22 @@ CREATE DATABASE financeiro;
-2. Configure as variaveis de ambiente da aplicacao:
+2. Configure as variáveis de ambiente da aplicação:
@@ -38 +38 @@ senha=admin
-Esse bootstrap automatico ocorre apenas em contexto local (base URL e datasource em `localhost`) e pode ser desativado com:
+Esse bootstrap automático ocorre apenas em contexto local (base URL e datasource em `localhost`) e pode ser desativado com:
@@ -44 +44 @@ APP_SECURITY_BOOTSTRAP_ADMIN_ENABLED=false
-4. Opcional para ambiente local: habilite o admin de teste e bootstrap de senhas somente se realmente necessario:
+4. Opcional para ambiente local: habilite o admin de teste e bootstrap de senhas somente se realmente necessário:
@@ -53 +53 @@ APP_SECURITY_BOOTSTRAP_PASSWORDS_ENABLED=true
-Se voce ja usava a base antiga da locadora, recomendacao: criar um banco novo (`financeiro`) para iniciar limpo.
+Se você já usava a base antiga da locadora, recomendação: criar um banco novo (`financeiro`) para iniciar limpo.
@@ -67 +67 @@ No Windows, use:
-## Padronizacao de codigo
+## Padronização de código
@@ -79 +79 @@ Acesse:
-- apos login: `http://localhost:8082/financeiro-pessoal`
+- após o login: `http://localhost:8082/financeiro-pessoal`
diff --git a/src/main/java/com/controle/finpessoal/config/ModelAttributes.java b/src/main/java/com/controle/finpessoal/config/ModelAttributes.java
index ec86fbe..ca69127 100644
--- a/src/main/java/com/controle/finpessoal/config/ModelAttributes.java
+++ b/src/main/java/com/controle/finpessoal/config/ModelAttributes.java
@@ -24 +24 @@ public class ModelAttributes {
-	private static final NavLink MOVIMENTO_CENTRAL_LINK = new NavLink("Central de movimentos",
+	private static final NavLink MOVIMENTO_CENTRAL_LINK = new NavLink("Central de movimentações",
@@ -109,0 +110,7 @@ public class ModelAttributes {
+	/**
+	 * Adiciona tema atributos.
+	 *
+	 * Organiza esta etapa do fluxo para manter a leitura do código mais direta e reutilizável.
+	 * @param model modelo usado para enviar atributos ao templaté
+	 * @param request requisicao recebida pela camada web
+	 */
@@ -156,0 +164 @@ public class ModelAttributes {
+	// Centraliza a etapa de Resolve página context label para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -167 +175 @@ public class ModelAttributes {
-			return "Movimentos";
+			return "Movimentações";
@@ -186,0 +195 @@ public class ModelAttributes {
+	// Centraliza a etapa de Executa tema do cookie para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -200,0 +210 @@ public class ModelAttributes {
+	// Centraliza a etapa de Monta rapido fluxo links para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -215,0 +226 @@ public class ModelAttributes {
+	// Centraliza a etapa de Monta notifications para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -224,0 +236 @@ public class ModelAttributes {
+	// Centraliza a etapa de Converte mes para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -232,0 +245,5 @@ public class ModelAttributes {
+/**
+ * Classe de configuração responsável por nav link.
+ *
+ * Ajusta comportamentos de infraestrutura, inicializacao e integracao usados pela aplicação.
+ */
@@ -233,0 +251,10 @@ public class ModelAttributes {
+		/**
+		 * Executa link.
+		 *
+		 * Organiza esta etapa do fluxo para manter a leitura do código mais direta e reutilizável.
+		 * @param label valor de apoio para label
+		 * @param path caminho envolvido no fluxo atual
+		 * @param icon valor de apoio para icon
+		 * @param description valor de apoio para description
+		 * @return resultado produzido pela operação
+		 */
@@ -237,0 +265,8 @@ public class ModelAttributes {
+		/**
+		 * Executa link.
+		 *
+		 * Organiza esta etapa do fluxo para manter a leitura do código mais direta e reutilizável.
+		 * @param label valor de apoio para label
+		 * @param path caminho envolvido no fluxo atual
+		 * @return resultado produzido pela operação
+		 */
@@ -242,0 +278,5 @@ public class ModelAttributes {
+/**
+ * Classe de configuração responsável por nav menu section.
+ *
+ * Ajusta comportamentos de infraestrutura, inicializacao e integracao usados pela aplicação.
+ */
@@ -245,0 +286,5 @@ public class ModelAttributes {
+/**
+ * Classe de configuração responsável por rapido fluxo link.
+ *
+ * Ajusta comportamentos de infraestrutura, inicializacao e integracao usados pela aplicação.
+ */
diff --git a/src/main/java/com/controle/finpessoal/controller/MovementController.java b/src/main/java/com/controle/finpessoal/controller/MovementController.java
index 52ac395..1eaefb6 100644
--- a/src/main/java/com/controle/finpessoal/controller/MovementController.java
+++ b/src/main/java/com/controle/finpessoal/controller/MovementController.java
@@ -25,0 +26,5 @@ import org.springframework.web.servlet.mvc.support.RedirectAttributes;
+/**
+ * Controller MVC responsável pelos fluxos de movimento.
+ *
+ * Exponibiliza endpoints web, prepara o Model usado pelos templatés Thymeleaf e encaminha as acoes da interface para a camada de serviço.
+ */
@@ -40,0 +46,9 @@ public class MovementController {
+	/**
+	 * Renderiza movimento.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param planItemId identificador usado para localizar o registro tratado
+	 * @param returnPath caminho envolvido no fluxo atual
+	 * @param model modelo usado para enviar atributos ao templaté
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -52,0 +67,11 @@ public class MovementController {
+	/**
+	 * Cria movimento.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param movementForm dados preenchidos no formulario da interface
+	 * @param bindingResult resultado acumulado das validações do formulario
+	 * @param returnPath caminho envolvido no fluxo atual
+	 * @param redirectAttributes atributos temporarios usados no redirecionamento
+	 * @param model modelo usado para enviar atributos ao templaté
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -59 +84 @@ public class MovementController {
-			redirectAttributes.addFlashAttribute("erro", "Lançamento não informado para registrar o movimento.");
+			redirectAttributes.addFlashAttribute("erro", "Lançamento não informado para registrar a movimentação.");
@@ -73 +98 @@ public class MovementController {
-			redirectAttributes.addFlashAttribute("sucesso", "Movimento registrado com sucesso.");
+			redirectAttributes.addFlashAttribute("sucesso", "Movimentação registrada com sucesso!");
@@ -81,0 +107,9 @@ public class MovementController {
+	/**
+	 * Renderiza histórico.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param planItemId identificador usado para localizar o registro tratado
+	 * @param returnPath caminho envolvido no fluxo atual
+	 * @param model modelo usado para enviar atributos ao templaté
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -94,0 +129,10 @@ public class MovementController {
+	/**
+	 * Exclui movimento post fallback.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param movementId identificador usado para localizar o registro tratado
+	 * @param planItemId identificador usado para localizar o registro tratado
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @param redirectAttributes atributos temporarios usados no redirecionamento
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -100,0 +145,10 @@ public class MovementController {
+	/**
+	 * Exclui movimento post.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param movementId identificador usado para localizar o registro tratado
+	 * @param planItemId identificador usado para localizar o registro tratado
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @param redirectAttributes atributos temporarios usados no redirecionamento
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -106,0 +161,10 @@ public class MovementController {
+	/**
+	 * Exclui movimento.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param movementId identificador usado para localizar o registro tratado
+	 * @param planItemId identificador usado para localizar o registro tratado
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @param redirectAttributes atributos temporarios usados no redirecionamento
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -112,0 +177 @@ public class MovementController {
+	// Centraliza a etapa de Renderiza delete movimento para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -117 +182 @@ public class MovementController {
-			redirectAttributes.addFlashAttribute("sucesso", "Baixa cancelada com sucesso.");
+			redirectAttributes.addFlashAttribute("sucesso", "Baixa cancelada com sucesso!");
@@ -126,0 +192 @@ public class MovementController {
+	// Centraliza a etapa de Renderiza movimento formulario modelo para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -140 +206 @@ public class MovementController {
-		model.addAttribute("movementSubmitLabel", incomeMovement ? "Salvar recebimento" : "Salvar movimento");
+		model.addAttribute("movementSubmitLabel", incomeMovement ? "Salvar recebimento" : "Salvar movimentação");
@@ -142,0 +209 @@ public class MovementController {
+	// Centraliza a etapa de Adiciona mes modelo para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -152,0 +220 @@ public class MovementController {
+	// Centraliza a etapa de Formata mes label para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -160,0 +229 @@ public class MovementController {
+	// Centraliza a etapa de Converte mes para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -171,0 +241 @@ public class MovementController {
+	// Centraliza a etapa de Normaliza return path para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -182,0 +253 @@ public class MovementController {
+	// Centraliza a etapa de Monta return url para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -188,0 +260 @@ public class MovementController {
+	// Centraliza a etapa de Resolve validação message para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -191 +263 @@ public class MovementController {
-			return "Informe os dados obrigatórios do movimento.";
+			return "Informe os dados obrigatórios da movimentação.";
@@ -194,2 +266,2 @@ public class MovementController {
-			case "accountId" -> "Selecione uma conta para registrar o movimento.";
-			case "paidAt" -> "Informe uma data válida para o movimento.";
+			case "accountId" -> "Selecione uma conta para registrar a movimentação.";
+			case "paidAt" -> "Informe uma data válida para a movimentação.";
@@ -197,2 +269,2 @@ public class MovementController {
-			default -> "Informe os dados obrigatórios do movimento.";
-		}).orElse("Informe os dados obrigatórios do movimento.");
+			default -> "Informe os dados obrigatórios da movimentação.";
+		}).orElse("Informe os dados obrigatórios da movimentação.");
diff --git a/src/main/java/com/controle/finpessoal/controller/PersonalFinanceController.java b/src/main/java/com/controle/finpessoal/controller/PersonalFinanceController.java
index 8f725f0..49dc642 100644
--- a/src/main/java/com/controle/finpessoal/controller/PersonalFinanceController.java
+++ b/src/main/java/com/controle/finpessoal/controller/PersonalFinanceController.java
@@ -80,0 +81,7 @@ import org.springframework.web.servlet.mvc.support.RedirectAttributes;
+/**
+ * Controller principal do módulo de Financeiro Pessoal.
+ *
+ * Centraliza as rotas que entregam as telas Thymeleaf do dashboard, planejámento,
+ * movimentações, cadastros e relatórios. Além de definir os endpoints, também monta
+ * o {@link Model} com filtros, formulários e visões agregadas consumidas pelo frontend.
+ */
@@ -128,0 +136,10 @@ public class PersonalFinanceController {
+	/**
+	 * Exibe o painel inicial do mês selecionado.
+	 *
+	 * A tela combina resumo do dashboard, últimas movimentações e indicadores comparativos
+	 * para que o usuário tenha uma visão rápida do mês antes de entrar nos detalhes.
+	 *
+	 * @param monthValue mês selecionado no formato {@code yyyy-MM}; quando ausente, usa o mês atual
+	 * @param model      modelo enviado ao templaté
+	 * @return templaté do painel financeiro
+	 */
@@ -141,0 +159,7 @@ public class PersonalFinanceController {
+	/**
+	 * Exibe a visão de calendário financeiro do mês.
+	 *
+	 * @param monthValue mês selecionado no filtro
+	 * @param model      modelo consumido pelo templaté
+	 * @return templaté do calendário
+	 */
@@ -149,0 +174,7 @@ public class PersonalFinanceController {
+	/**
+	 * Exibe a tela de metas financeiras do mês.
+	 *
+	 * @param monthValue mês de referência da navegação
+	 * @param model      modelo usado pelo templaté
+	 * @return templaté de metas
+	 */
@@ -158,0 +190,10 @@ public class PersonalFinanceController {
+	/**
+	 * Exibe a tela de planejámento financeiro do mês.
+	 *
+	 * Carrega metas de orçamento, analytics e formulários usados para cadastrar
+	 * receitas, despesas e templatés do período.
+	 *
+	 * @param monthValue mês selecionado
+	 * @param model      modelo enviado ao templaté
+	 * @return templaté de planejámento
+	 */
@@ -167,0 +209,7 @@ public class PersonalFinanceController {
+	/**
+	 * Redireciona a entrada de cadastros para a tela padrão de usuários.
+	 *
+	 * @param monthValue mês de navegação atual
+	 * @param model      modelo da requisição; não é usado porque o método apenas redireciona
+	 * @return redirecionamento para o catálogo de usuários
+	 */
@@ -173,0 +222,10 @@ public class PersonalFinanceController {
+	/**
+	 * Exibe o cadastro de usuários/pessoas do sistema.
+	 *
+	 * A tela também controla se o usuário logado tem permissão para manutenção do
+	 * cadastro antes de preparar o formulário de inclusão/edição.
+	 *
+	 * @param monthValue mês de navegação
+	 * @param model      modelo enviado ao templaté
+	 * @return templaté de cadastro de usuários
+	 */
@@ -185,0 +244,7 @@ public class PersonalFinanceController {
+	/**
+	 * Executa c ad as tr op es só as.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -191,0 +257,7 @@ public class PersonalFinanceController {
+	/**
+	 * Exibe o catálogo de grupos de catégoria.
+	 *
+	 * @param monthValue mês de navegação
+	 * @param model      modelo enviado ao templaté
+	 * @return templaté de grupos de catégoria
+	 */
@@ -197,0 +270,8 @@ public class PersonalFinanceController {
+	/**
+	 * Executa c ad as tr os ub cl as si fi ca co es.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @param model modelo usado para enviar atributos ao templaté
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -204,0 +285,7 @@ public class PersonalFinanceController {
+	/**
+	 * Executa r ed ir ec tc ad as tr ot ip os.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -210,0 +298,2 @@ public class PersonalFinanceController {
+	// Reaproveita o mesmo preparo de tela para grupos raiz e subclassificações,
+	// mudando apenas o recorte exibido e o caminho de retorno da interface.
@@ -228,0 +318,7 @@ public class PersonalFinanceController {
+	/**
+	 * Exibe o cadastro de contas financeiras.
+	 *
+	 * @param monthValue mês de navegação
+	 * @param model      modelo com lista de contas e formulário
+	 * @return templaté de contas
+	 */
@@ -237,0 +334,6 @@ public class PersonalFinanceController {
+	/**
+	 * Mantém compatibilidade com o caminho antigo de cadastro de cartões.
+	 *
+	 * @param monthValue mês de navegação
+	 * @return redirecionamento para o módulo atual de cartões
+	 */
@@ -243,0 +346,7 @@ public class PersonalFinanceController {
+	/**
+	 * Executa l an ca me nt oi te ns.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -249,0 +359,7 @@ public class PersonalFinanceController {
+	/**
+	 * Executa e xp or ta rl an ca me nt os pd f.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @return resposta HTTP pronta para download ou consumo da interface
+	 */
@@ -261,0 +378,7 @@ public class PersonalFinanceController {
+	/**
+	 * Executa e xp or ta rl an ca me nt os ex ce l.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @return resposta HTTP pronta para download ou consumo da interface
+	 */
@@ -273,0 +397,8 @@ public class PersonalFinanceController {
+	/**
+	 * Executa l an ca me nt on ov oi te m.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @param model modelo usado para enviar atributos ao templaté
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -284,0 +416,8 @@ public class PersonalFinanceController {
+	/**
+	 * Executa l an ca me nt ot ot ai sg ru po.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @param model modelo usado para enviar atributos ao templaté
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -291,0 +431,8 @@ public class PersonalFinanceController {
+	/**
+	 * Executa l an ca me nt oh is to ri co.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @param model modelo usado para enviar atributos ao templaté
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -300,0 +448,7 @@ public class PersonalFinanceController {
+	/**
+	 * Executa r ed ir ec tl an ca me nt oi te ns.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -305,0 +460,7 @@ public class PersonalFinanceController {
+	/**
+	 * Executa r ed ir ec tl an ca me nt on ov oi te m.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -310,0 +472,7 @@ public class PersonalFinanceController {
+	/**
+	 * Executa r ed ir ec tl an ca me nt ot ot ai sg ru po.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -315,0 +484,7 @@ public class PersonalFinanceController {
+	/**
+	 * Executa r ed ir ec tl an ca me nt oh is to ri co.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -320,0 +496,16 @@ public class PersonalFinanceController {
+	/**
+	 * Exibe a central de movimentações do mês com filtros avançados.
+	 *
+	 * A tela combina itens ainda abertos do planejámento com o histórico de baixas já
+	 * realizadas, permitindo filtrar por tipo, status, conta, responsável e texto livre.
+	 *
+	 * @param monthValue           mês selecionado
+	 * @param itemTypeFilterValue  filtro de tipo do item
+	 * @param statusFilterValue    filtro de status
+	 * @param accountIdFilter      conta selecionada
+	 * @param ownerUserIdFilter    usuário selecionado
+	 * @param searchValue          texto de busca
+	 * @param focusValue           seção da interface que deve receber foco visual
+	 * @param model                modelo consumido pelo templaté
+	 * @return templaté de movimentações
+	 */
@@ -334,0 +526,11 @@ public class PersonalFinanceController {
+	/**
+	 * Executa m ov im en to sr ec ei ta s.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @param statusFilterValue valor recebido para processamento
+	 * @param ownerUserIdFilter filtro aplicado no recorte atual
+	 * @param searchValue valor recebido para processamento
+	 * @param model modelo usado para enviar atributos ao templaté
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -344,0 +547,11 @@ public class PersonalFinanceController {
+	/**
+	 * Executa m ov im en to sd es pe sa s.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @param statusFilterValue valor recebido para processamento
+	 * @param ownerUserIdFilter filtro aplicado no recorte atual
+	 * @param searchValue valor recebido para processamento
+	 * @param model modelo usado para enviar atributos ao templaté
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -354,0 +568,8 @@ public class PersonalFinanceController {
+	/**
+	 * Executa m ov im en to sc on ta sa pa ga r.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @param ownerUserIdFilter filtro aplicado no recorte atual
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -361,0 +583,8 @@ public class PersonalFinanceController {
+	/**
+	 * Executa m ov im en to sb ai xa rc on ta s.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @param ownerUserIdFilter filtro aplicado no recorte atual
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -368,0 +598,8 @@ public class PersonalFinanceController {
+	/**
+	 * Executa m ov im en to sc on ta sp ag as.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @param ownerUserIdFilter filtro aplicado no recorte atual
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -375,0 +613,8 @@ public class PersonalFinanceController {
+	/**
+	 * Executa m ov im en to sc on ta sa re ce be r.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @param ownerUserIdFilter filtro aplicado no recorte atual
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -382,0 +628,8 @@ public class PersonalFinanceController {
+	/**
+	 * Executa m ov im en to sc on ta sr ec eb identificador as.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @param ownerUserIdFilter filtro aplicado no recorte atual
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -389,0 +643,2 @@ public class PersonalFinanceController {
+	// Concentra a montagem da tela de movimentações completa para reaproveitar a mesma
+	// preparação em atalhos e variações de navegação sem duplicar filtros e totais.
@@ -445,0 +701,2 @@ public class PersonalFinanceController {
+	// Reaproveita a lógica de filtros das páginas simplificadas de receitas e despesas,
+	// mantendo cards, totais e atalhos consistentes com a tela principal.
@@ -523,0 +781,6 @@ public class PersonalFinanceController {
+	/**
+	 * Redireciona a entrada de relatórios para o relatório de fluxo.
+	 *
+	 * @param monthValue mês inicial de contexto
+	 * @return redirecionamento para o relatório padrão
+	 */
@@ -528,0 +792,13 @@ public class PersonalFinanceController {
+	/**
+	 * Executa r el at or io me ns al.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @param startMonthValue mes de referência usado no fluxo atual
+	 * @param endMonthValue mes de referência usado no fluxo atual
+	 * @param ownerUserIdFilter filtro aplicado no recorte atual
+	 * @param accountIdFilter filtro aplicado no recorte atual
+	 * @param classificationFilterValue valor recebido para processamento
+	 * @param statusFilterValue valor recebido para processamento
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -540,0 +817,13 @@ public class PersonalFinanceController {
+	/**
+	 * Exibe o relatório consolidado por período.
+	 *
+	 * @param startMonthValue        mês inicial do intervalo
+	 * @param endMonthValue          mês final do intervalo
+	 * @param monthValue             mês de fallback quando o intervalo não for informado
+	 * @param ownerUserIdFilter      filtro por usuário
+	 * @param accountIdFilter        filtro por conta
+	 * @param classificationFilterValue filtro por classificação financeira
+	 * @param statusFilterValue      filtro por status
+	 * @param model                  modelo consumido pelo templaté
+	 * @return templaté do relatório por período
+	 */
@@ -557,0 +847,13 @@ public class PersonalFinanceController {
+	/**
+	 * Exibe o relatório de fluxo de caixa com comparação ao relatório mensal.
+	 *
+	 * @param startMonthValue           mês inicial do período
+	 * @param endMonthValue             mês final do período
+	 * @param monthValue                mês de fallback
+	 * @param ownerUserIdFilter         filtro por usuário
+	 * @param accountIdFilter           filtro por conta
+	 * @param classificationFilterValue filtro por classificação
+	 * @param statusFilterValue         filtro por status
+	 * @param model                     modelo do templaté
+	 * @return templaté do relatório de fluxo
+	 */
@@ -578,0 +881,13 @@ public class PersonalFinanceController {
+	/**
+	 * Exibe o relatório de performance por catégorias/grupos.
+	 *
+	 * @param startMonthValue           mês inicial do período
+	 * @param endMonthValue             mês final do período
+	 * @param monthValue                mês de fallback
+	 * @param ownerUserIdFilter         filtro por usuário
+	 * @param accountIdFilter           filtro por conta
+	 * @param classificationFilterValue filtro por classificação
+	 * @param statusFilterValue         filtro por status
+	 * @param model                     modelo do templaté
+	 * @return templaté do relatório por catégorias
+	 */
@@ -595,0 +911,8 @@ public class PersonalFinanceController {
+	/**
+	 * Executa r el at or io bi.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @param model modelo usado para enviar atributos ao templaté
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -606,0 +930,13 @@ public class PersonalFinanceController {
+	/**
+	 * Executa e xp or ta rr el at or io pe ri od o.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param startMonthValue mes de referência usado no fluxo atual
+	 * @param endMonthValue mes de referência usado no fluxo atual
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @param ownerUserIdFilter filtro aplicado no recorte atual
+	 * @param accountIdFilter filtro aplicado no recorte atual
+	 * @param classificationFilterValue valor recebido para processamento
+	 * @param statusFilterValue valor recebido para processamento
+	 * @return resposta HTTP pronta para download ou consumo da interface
+	 */
@@ -625,0 +962,11 @@ public class PersonalFinanceController {
+	/**
+	 * Executa e xp or ta rr el at or io me ns al pd f.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @param ownerUserIdFilter filtro aplicado no recorte atual
+	 * @param accountIdFilter filtro aplicado no recorte atual
+	 * @param classificationFilterValue valor recebido para processamento
+	 * @param statusFilterValue valor recebido para processamento
+	 * @return resposta HTTP pronta para download ou consumo da interface
+	 */
@@ -642,0 +990,11 @@ public class PersonalFinanceController {
+	/**
+	 * Executa e xp or ta rr el at or io me ns al ex ce l.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @param ownerUserIdFilter filtro aplicado no recorte atual
+	 * @param accountIdFilter filtro aplicado no recorte atual
+	 * @param classificationFilterValue valor recebido para processamento
+	 * @param statusFilterValue valor recebido para processamento
+	 * @return resposta HTTP pronta para download ou consumo da interface
+	 */
@@ -659,0 +1018,13 @@ public class PersonalFinanceController {
+	/**
+	 * Executa e xp or ta rr el at or io pe ri od op df.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param startMonthValue mes de referência usado no fluxo atual
+	 * @param endMonthValue mes de referência usado no fluxo atual
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @param ownerUserIdFilter filtro aplicado no recorte atual
+	 * @param accountIdFilter filtro aplicado no recorte atual
+	 * @param classificationFilterValue valor recebido para processamento
+	 * @param statusFilterValue valor recebido para processamento
+	 * @return resposta HTTP pronta para download ou consumo da interface
+	 */
@@ -679,0 +1051,13 @@ public class PersonalFinanceController {
+	/**
+	 * Executa e xp or ta rr el at or io pe ri od oe xc el.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param startMonthValue mes de referência usado no fluxo atual
+	 * @param endMonthValue mes de referência usado no fluxo atual
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @param ownerUserIdFilter filtro aplicado no recorte atual
+	 * @param accountIdFilter filtro aplicado no recorte atual
+	 * @param classificationFilterValue valor recebido para processamento
+	 * @param statusFilterValue valor recebido para processamento
+	 * @return resposta HTTP pronta para download ou consumo da interface
+	 */
@@ -699,0 +1084,13 @@ public class PersonalFinanceController {
+	/**
+	 * Executa e xp or ta rr el at or io fl ux op df.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param startMonthValue mes de referência usado no fluxo atual
+	 * @param endMonthValue mes de referência usado no fluxo atual
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @param ownerUserIdFilter filtro aplicado no recorte atual
+	 * @param accountIdFilter filtro aplicado no recorte atual
+	 * @param classificationFilterValue valor recebido para processamento
+	 * @param statusFilterValue valor recebido para processamento
+	 * @return resposta HTTP pronta para download ou consumo da interface
+	 */
@@ -719,0 +1117,13 @@ public class PersonalFinanceController {
+	/**
+	 * Executa e xp or ta rr el at or io fl ux oe xc el.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param startMonthValue mes de referência usado no fluxo atual
+	 * @param endMonthValue mes de referência usado no fluxo atual
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @param ownerUserIdFilter filtro aplicado no recorte atual
+	 * @param accountIdFilter filtro aplicado no recorte atual
+	 * @param classificationFilterValue valor recebido para processamento
+	 * @param statusFilterValue valor recebido para processamento
+	 * @return resposta HTTP pronta para download ou consumo da interface
+	 */
@@ -739,0 +1150,13 @@ public class PersonalFinanceController {
+	/**
+	 * Executa e xp or ta rr el at or io ca te go ri as pd f.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param startMonthValue mes de referência usado no fluxo atual
+	 * @param endMonthValue mes de referência usado no fluxo atual
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @param ownerUserIdFilter filtro aplicado no recorte atual
+	 * @param accountIdFilter filtro aplicado no recorte atual
+	 * @param classificationFilterValue valor recebido para processamento
+	 * @param statusFilterValue valor recebido para processamento
+	 * @return resposta HTTP pronta para download ou consumo da interface
+	 */
@@ -759,0 +1183,13 @@ public class PersonalFinanceController {
+	/**
+	 * Executa e xp or ta rr el at or io ca te go ri as ex ce l.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param startMonthValue mes de referência usado no fluxo atual
+	 * @param endMonthValue mes de referência usado no fluxo atual
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @param ownerUserIdFilter filtro aplicado no recorte atual
+	 * @param accountIdFilter filtro aplicado no recorte atual
+	 * @param classificationFilterValue valor recebido para processamento
+	 * @param statusFilterValue valor recebido para processamento
+	 * @return resposta HTTP pronta para download ou consumo da interface
+	 */
@@ -779,0 +1216,9 @@ public class PersonalFinanceController {
+	/**
+	 * Executa c re at eu se r.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param form dados preenchidos no formulario da interface
+	 * @param bindingResult resultado acumulado das validações do formulario
+	 * @param redirectAttributes atributos temporarios usados no redirecionamento
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -785 +1230 @@ public class PersonalFinanceController {
-				"Informe dados válidos para o usuário.", PATH_CADASTROS_USUARIOS, "Usuário cadastrado com sucesso.",
+				"Informe dados válidos para o usuário.", PATH_CADASTROS_USUARIOS, "Usuário cadastrado com sucesso!",
@@ -789,0 +1235,10 @@ public class PersonalFinanceController {
+	/**
+	 * Executa u pd at eu se r.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param id identificador usado para localizar o registro tratado
+	 * @param form dados preenchidos no formulario da interface
+	 * @param bindingResult resultado acumulado das validações do formulario
+	 * @param redirectAttributes atributos temporarios usados no redirecionamento
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -795 +1250 @@ public class PersonalFinanceController {
-				"Informe dados válidos para o usuário.", PATH_CADASTROS_USUARIOS, "Usuário atualizado com sucesso.",
+				"Informe dados válidos para o usuário.", PATH_CADASTROS_USUARIOS, "Usuário atualizado com sucesso!",
@@ -799,0 +1255,9 @@ public class PersonalFinanceController {
+	/**
+	 * Executa i na ct iv at eu se r.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param id identificador usado para localizar o registro tratado
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @param redirectAttributes atributos temporarios usados no redirecionamento
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -805,3 +1269,12 @@ public class PersonalFinanceController {
-				"Usuário inativado com sucesso.", () -> usuarioService.inativarUsuario(id));
-	}
-
+				"Usuário inativado com sucesso!", () -> usuarioService.inativarUsuario(id));
+	}
+
+	/**
+	 * Executa r ea ct iv at eu se r.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param id identificador usado para localizar o registro tratado
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @param redirectAttributes atributos temporarios usados no redirecionamento
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -813,3 +1286,13 @@ public class PersonalFinanceController {
-				"Usuário reativado com sucesso.", () -> usuarioService.reativarUsuario(id));
-	}
-
+				"Usuário reativado com sucesso!", () -> usuarioService.reativarUsuario(id));
+	}
+
+	/**
+	 * Executa c re at ec at eg or yg ro up.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param form dados preenchidos no formulario da interface
+	 * @param bindingResult resultado acumulado das validações do formulario
+	 * @param retorno valor de apoio para retorno
+	 * @param redirectAttributes atributos temporarios usados no redirecionamento
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -823 +1306 @@ public class PersonalFinanceController {
-				"Informe dados válidos para o grupo.", redirectPath, "Grupo cadastrado com sucesso.",
+				"Informe dados válidos para o grupo.", redirectPath, "Grupo cadastrado com sucesso!",
@@ -826,0 +1310,11 @@ public class PersonalFinanceController {
+	/**
+	 * Executa u pd at ec at eg or yg ro up.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param id identificador usado para localizar o registro tratado
+	 * @param form dados preenchidos no formulario da interface
+	 * @param bindingResult resultado acumulado das validações do formulario
+	 * @param retorno valor de apoio para retorno
+	 * @param redirectAttributes atributos temporarios usados no redirecionamento
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -835 +1329 @@ public class PersonalFinanceController {
-				"Informe dados válidos para o grupo.", redirectPath, "Grupo atualizado com sucesso.",
+				"Informe dados válidos para o grupo.", redirectPath, "Grupo atualizado com sucesso!",
@@ -838,0 +1333,10 @@ public class PersonalFinanceController {
+	/**
+	 * Executa d el et ec at eg or yg ro up.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param id identificador usado para localizar o registro tratado
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @param retorno valor de apoio para retorno
+	 * @param redirectAttributes atributos temporarios usados no redirecionamento
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -845 +1349 @@ public class PersonalFinanceController {
-		return handleAction(selectedMonth, redirectAttributes, redirectPath, "Grupo excluído com sucesso.",
+		return handleAction(selectedMonth, redirectAttributes, redirectPath, "Grupo excluído com sucesso!",
@@ -848,0 +1353,10 @@ public class PersonalFinanceController {
+	/**
+	 * Executa i na ct iv at ec at eg or yg ro up.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param id identificador usado para localizar o registro tratado
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @param retorno valor de apoio para retorno
+	 * @param redirectAttributes atributos temporarios usados no redirecionamento
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -855 +1369 @@ public class PersonalFinanceController {
-		return handleAction(selectedMonth, redirectAttributes, redirectPath, "Grupo inativado com sucesso.",
+		return handleAction(selectedMonth, redirectAttributes, redirectPath, "Grupo inativado com sucesso!",
@@ -858,0 +1373,10 @@ public class PersonalFinanceController {
+	/**
+	 * Executa r ea ct iv at ec at eg or yg ro up.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param id identificador usado para localizar o registro tratado
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @param retorno valor de apoio para retorno
+	 * @param redirectAttributes atributos temporarios usados no redirecionamento
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -865 +1389 @@ public class PersonalFinanceController {
-		return handleAction(selectedMonth, redirectAttributes, redirectPath, "Grupo reativado com sucesso.",
+		return handleAction(selectedMonth, redirectAttributes, redirectPath, "Grupo reativado com sucesso!",
@@ -868,0 +1393,9 @@ public class PersonalFinanceController {
+	/**
+	 * Executa c re at ea cc ou nt.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param form dados preenchidos no formulario da interface
+	 * @param bindingResult resultado acumulado das validações do formulario
+	 * @param redirectAttributes atributos temporarios usados no redirecionamento
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -874 +1407 @@ public class PersonalFinanceController {
-				"Informe dados válidos para a conta.", PATH_CADASTROS_CONTAS, "Conta cadastrada com sucesso.",
+				"Informe dados válidos para a conta.", PATH_CADASTROS_CONTAS, "Conta cadastrada com sucesso!",
@@ -877,0 +1411,10 @@ public class PersonalFinanceController {
+	/**
+	 * Executa u pd at ea cc ou nt.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param id identificador usado para localizar o registro tratado
+	 * @param form dados preenchidos no formulario da interface
+	 * @param bindingResult resultado acumulado das validações do formulario
+	 * @param redirectAttributes atributos temporarios usados no redirecionamento
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -884 +1427 @@ public class PersonalFinanceController {
-				"Informe dados válidos para a conta.", PATH_CADASTROS_CONTAS, "Conta atualizada com sucesso.",
+				"Informe dados válidos para a conta.", PATH_CADASTROS_CONTAS, "Conta atualizada com sucesso!",
@@ -888,0 +1432,9 @@ public class PersonalFinanceController {
+	/**
+	 * Executa i na ct iv at ea cc ou nt.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param id identificador usado para localizar o registro tratado
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @param redirectAttributes atributos temporarios usados no redirecionamento
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -893 +1445 @@ public class PersonalFinanceController {
-		return handleAction(selectedMonth, redirectAttributes, PATH_CADASTROS_CONTAS, "Conta inativada com sucesso.",
+		return handleAction(selectedMonth, redirectAttributes, PATH_CADASTROS_CONTAS, "Conta inativada com sucesso!",
@@ -896,0 +1449,9 @@ public class PersonalFinanceController {
+	/**
+	 * Executa r ea ct iv at ea cc ou nt.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param id identificador usado para localizar o registro tratado
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @param redirectAttributes atributos temporarios usados no redirecionamento
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -901 +1462 @@ public class PersonalFinanceController {
-		return handleAction(selectedMonth, redirectAttributes, PATH_CADASTROS_CONTAS, "Conta reativada com sucesso.",
+		return handleAction(selectedMonth, redirectAttributes, PATH_CADASTROS_CONTAS, "Conta reativada com sucesso!",
@@ -904,0 +1466,10 @@ public class PersonalFinanceController {
+	/**
+	 * Executa u ps er tp la ni te m.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param form dados preenchidos no formulario da interface
+	 * @param bindingResult resultado acumulado das validações do formulario
+	 * @param retorno valor de apoio para retorno
+	 * @param redirectAttributes atributos temporarios usados no redirecionamento
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -915 +1486 @@ public class PersonalFinanceController {
-				novoItem ? "Lançamento criado com sucesso." : "Lançamento atualizado com sucesso.",
+				novoItem ? "Lançamento criado com sucesso!" : "Lançamento atualizado com sucesso!",
@@ -919,0 +1491,10 @@ public class PersonalFinanceController {
+	/**
+	 * Executa c re at ei ns ta ll me nt te mp la te.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param form dados preenchidos no formulario da interface
+	 * @param bindingResult resultado acumulado das validações do formulario
+	 * @param retorno valor de apoio para retorno
+	 * @param redirectAttributes atributos temporarios usados no redirecionamento
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -930 +1511 @@ public class PersonalFinanceController {
-		executeWithFeedback(redirectAttributes, "Lançamento parcelado criado com sucesso.",
+		executeWithFeedback(redirectAttributes, "Lançamento parcelado criado com sucesso!",
@@ -934,0 +1516,10 @@ public class PersonalFinanceController {
+	/**
+	 * Executa c re at er ec ur ri ng te mp la te.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param form dados preenchidos no formulario da interface
+	 * @param bindingResult resultado acumulado das validações do formulario
+	 * @param retorno valor de apoio para retorno
+	 * @param redirectAttributes atributos temporarios usados no redirecionamento
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -945 +1536 @@ public class PersonalFinanceController {
-		executeWithFeedback(redirectAttributes, "Lançamento recorrente criado com sucesso.",
+		executeWithFeedback(redirectAttributes, "Lançamento recorrente criado com sucesso!",
@@ -949,0 +1541,10 @@ public class PersonalFinanceController {
+	/**
+	 * Executa i na ct iv at et em pl at e.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param id identificador usado para localizar o registro tratado
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @param retorno valor de apoio para retorno
+	 * @param redirectAttributes atributos temporarios usados no redirecionamento
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -955 +1556 @@ public class PersonalFinanceController {
-		executeWithFeedback(redirectAttributes, "Modelo inativado com sucesso.",
+		executeWithFeedback(redirectAttributes, "Modelo inativado com sucesso!",
@@ -959,0 +1561,11 @@ public class PersonalFinanceController {
+	/**
+	 * Executa c lo se te mp la te.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param id identificador usado para localizar o registro tratado
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @param retorno valor de apoio para retorno
+	 * @param reason valor de apoio para reason
+	 * @param redirectAttributes atributos temporarios usados no redirecionamento
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -965 +1577 @@ public class PersonalFinanceController {
-		executeWithFeedback(redirectAttributes, "Recorrência encerrada com sucesso.",
+		executeWithFeedback(redirectAttributes, "Recorrência encerrada com sucesso!",
@@ -969,0 +1582,11 @@ public class PersonalFinanceController {
+	/**
+	 * Executa s et tl et em pl at ee ar ly.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param id identificador usado para localizar o registro tratado
+	 * @param form dados preenchidos no formulario da interface
+	 * @param bindingResult resultado acumulado das validações do formulario
+	 * @param retorno valor de apoio para retorno
+	 * @param redirectAttributes atributos temporarios usados no redirecionamento
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -980 +1603 @@ public class PersonalFinanceController {
-		executeWithFeedback(redirectAttributes, "Parcelamento quitado antecipadamente com sucesso.",
+		executeWithFeedback(redirectAttributes, "Parcelamento quitado antecipadamente com sucesso!",
@@ -984,0 +1608,10 @@ public class PersonalFinanceController {
+	/**
+	 * Executa i na ct iv at ep la ni te m.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param id identificador usado para localizar o registro tratado
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @param retorno valor de apoio para retorno
+	 * @param redirectAttributes atributos temporarios usados no redirecionamento
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -990 +1623 @@ public class PersonalFinanceController {
-		executeWithFeedback(redirectAttributes, "Item inativado com sucesso.",
+		executeWithFeedback(redirectAttributes, "Item inativado com sucesso!",
@@ -994,0 +1628,10 @@ public class PersonalFinanceController {
+	/**
+	 * Executa d el et ep la ni te m.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param id identificador usado para localizar o registro tratado
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @param retorno valor de apoio para retorno
+	 * @param redirectAttributes atributos temporarios usados no redirecionamento
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -999 +1642 @@ public class PersonalFinanceController {
-		executeWithFeedback(redirectAttributes, "Lançamento excluído com sucesso.",
+		executeWithFeedback(redirectAttributes, "Lançamento excluído com sucesso!",
@@ -1003,0 +1647,15 @@ public class PersonalFinanceController {
+	/**
+	 * Executa r eg is te rp ay me nt.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param paymentForm dados preenchidos no formulario da interface
+	 * @param bindingResult resultado acumulado das validações do formulario
+	 * @param retorno valor de apoio para retorno
+	 * @param itemTypeFilterValue valor recebido para processamento
+	 * @param statusFilterValue valor recebido para processamento
+	 * @param accountFilterValue valor recebido para processamento
+	 * @param ownerUserFilterValue valor recebido para processamento
+	 * @param textSearchValue valor recebido para processamento
+	 * @param redirectAttributes atributos temporarios usados no redirecionamento
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -1022 +1680 @@ public class PersonalFinanceController {
-				editing ? "Lançamento atualizado com sucesso." : "Pagamento registrado com sucesso.",
+				editing ? "Lançamento atualizado com sucesso!" : "Pagamento registrado com sucesso!",
@@ -1028,0 +1687,15 @@ public class PersonalFinanceController {
+	/**
+	 * Executa d el et ep ay me nt.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param id identificador usado para localizar o registro tratado
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @param retorno valor de apoio para retorno
+	 * @param itemTypeFilterValue valor recebido para processamento
+	 * @param statusFilterValue valor recebido para processamento
+	 * @param accountFilterValue valor recebido para processamento
+	 * @param ownerUserFilterValue valor recebido para processamento
+	 * @param textSearchValue valor recebido para processamento
+	 * @param redirectAttributes atributos temporarios usados no redirecionamento
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -1039 +1712 @@ public class PersonalFinanceController {
-		executeWithFeedback(redirectAttributes, "Baixa cancelada com sucesso.",
+		executeWithFeedback(redirectAttributes, "Baixa cancelada com sucesso!",
@@ -1044,0 +1718,15 @@ public class PersonalFinanceController {
+	/**
+	 * Executa c an ce lp ay me nt.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param id identificador usado para localizar o registro tratado
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @param retorno valor de apoio para retorno
+	 * @param itemTypeFilterValue valor recebido para processamento
+	 * @param statusFilterValue valor recebido para processamento
+	 * @param accountFilterValue valor recebido para processamento
+	 * @param ownerUserFilterValue valor recebido para processamento
+	 * @param textSearchValue valor recebido para processamento
+	 * @param redirectAttributes atributos temporarios usados no redirecionamento
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -1055 +1743 @@ public class PersonalFinanceController {
-		executeWithFeedback(redirectAttributes, "Baixa cancelada com sucesso.",
+		executeWithFeedback(redirectAttributes, "Baixa cancelada com sucesso!",
@@ -1060,0 +1749,15 @@ public class PersonalFinanceController {
+	/**
+	 * Executa q ui ck pa y.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param id identificador usado para localizar o registro tratado
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @param retorno valor de apoio para retorno
+	 * @param itemTypeFilterValue valor recebido para processamento
+	 * @param statusFilterValue valor recebido para processamento
+	 * @param accountFilterValue valor recebido para processamento
+	 * @param ownerUserFilterValue valor recebido para processamento
+	 * @param textSearchValue valor recebido para processamento
+	 * @param redirectAttributes atributos temporarios usados no redirecionamento
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -1071 +1774 @@ public class PersonalFinanceController {
-		executeWithFeedback(redirectAttributes, "Pagamento registrado com sucesso.",
+		executeWithFeedback(redirectAttributes, "Pagamento registrado com sucesso!",
@@ -1076,0 +1780 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa a dd mo nt hm od el para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1090,0 +1795 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa s or tr ec en tm ov em en ts para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1101,0 +1807 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa a pp ly da sh bo ar da na ly ti cs mo de l para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1116,0 +1823 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa a dd pl an mo de l para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1120,0 +1828 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa a dd pl an mo de l para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1155,0 +1864 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa a dd us er fo rm mo de l para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1162,0 +1872 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa a dd ca te go ry gr ou pf or mm od el para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1172,0 +1883 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa a dd ac co un tf or mm od el para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1180,0 +1892 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa a dd pl an it em fo rm mo de l para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1194,0 +1907 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa a dd sa vi ng sg oa lf or mm od el para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1202,0 +1916 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa a dd bu dg et go al fo rm mo de l para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1210,0 +1925 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa a dd in st al lm en tt em pl at ef or mm od el para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1219,0 +1935 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa a dd re cu rr in gt em pl at ef or mm od el para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1230,0 +1947 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa a dd ea rl ys et tl em en tf or mm od el para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1238,0 +1956 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa a dd pa ym en tf or mm od el para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1253,0 +1972 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa r es ol ve mo nt hl yr ep or tq ue ry para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1259,0 +1979 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa r es ol ve pe ri od re po rt qu er y para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1266,0 +1987 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa l oa dm on th ly re po rt co nt ex t para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1275,0 +1997 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa l oa dp er io dr ep or tc on te xt para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1284,0 +2007 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa l oa dc as hf lo wr ep or tc on te xt para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1294,0 +2018 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa l oa dc at eg or yp er fo rm an ce re po rt co nt ex t para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1304,0 +2029 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa a pp ly re po rt mo de l para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1311,0 +2037 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa a pp ly pe ri od re po rt mo de l para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1318,0 +2045 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa b ui ld bi na ry do wn lo ad re sp on se para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1324,0 +2052 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa b ui ld te xt do wn lo ad re sp on se para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1331,0 +2060 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa b ui ld mo nt hl yr ep or ts ub ti tl e para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1335,0 +2065 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa b ui ld mo nt hl yr ep or tf il en am e para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1339,0 +2070 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa b ui ld pe ri od re po rt fi le na me para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1344,0 +2076 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa r ed ir ec tt or el at or io fl ux o para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1362,0 +2095 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa a pp en dp os it iv eq ue ry pa ra m para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1370,0 +2104 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa a pp en dn or ma li ze dq ue ry pa ra m para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1378,0 +2113 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa a dd re po rt fi lt er mo de l para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1390,0 +2126 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa r es ol ve re po rt fi lt er se le ct io n para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1407,0 +2144 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa r es ol ve va li du se rf il te r para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1416,0 +2154 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa r es ol ve va li da cc ou nt fi lt er para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1425,0 +2164 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa r es ol ve re po rt us er la be l para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1433,0 +2173 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa r es ol ve re po rt ac co un tl ab el para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1442,0 +2183 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa n or ma li ze re po rt cl as si fi ca ti on fi lt er para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1453,0 +2195 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa n or ma li ze re po rt st at us fi lt er para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1464,0 +2207 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa p ar se re po rt cl as si fi ca ti on para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1475,0 +2219 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa p ar se re po rt st at us para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1486,0 +2231 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa b ui ld re po rt su bt it le para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1509,0 +2255 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa h as va li da ti on er ro rs para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1520,0 +2267 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa r es ol ve va li da ti on me ss ag e para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1529 +2276 @@ public class PersonalFinanceController {
-				case "planItemId" -> "Nao foi possivel identificar o lancamento para registrar o pagamento.";
+				case "planItemId" -> "Não foi possível identificar o lançamento para registrar o pagamento.";
@@ -1531 +2278 @@ public class PersonalFinanceController {
-				case "paidAt" -> "Informe uma data de pagamento valida.";
+				case "paidAt" -> "Informe uma data de pagamento válida.";
@@ -1540,0 +2288 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa h an dl ev al identificador at ed ac ti on para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1549,0 +2298 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa h an dl ea ct io n para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1555,0 +2305 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa e xe cu te wi th fe ed ba ck para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1573,0 +2324 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa r ed ir ec tt op ag e para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1578,0 +2330 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa r ed ir ec tt om ov im en to sw it hf il te rs para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1615,0 +2368 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa r ed ir ec tm ov im en to ss ho rt cu t para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1643,0 +2397 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa i sm ov em en tr ed ir ec tp at h para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1648,0 +2403 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa r ed ir ec tt ol an ca me nt os pa ge para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1654,0 +2410 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa r es ol ve sa fe pa th para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1669,0 +2426 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa s or ti te ms by du ed at e para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1682,0 +2440 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa m er ge an ds or ti te ms para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1694,0 +2453 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa r es ol ve da sh bo ar dd ue da te para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1712,0 +2472 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa c al cu la te mo ve me nt me tr ic s para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1777,0 +2538 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa f il te rm ov em en ts para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1804,0 +2566 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa r es ol ve mo ve me nt fi lt er s para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1811,0 +2574 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa n or ma li ze mo ve me nt it em ty pe fi lt er para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1827,0 +2591 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa n or ma li ze mo ve me nt st at us fi lt er para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1838,0 +2603 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa n or ma li ze po si ti ve identificador fi lt er para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1845,0 +2611 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa n or ma li ze mo ve me nt te xt fi lt er para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1852,0 +2619 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa m at ch es mo ve me nt it em ty pe para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1865,0 +2633 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa m at ch es mo ve me nt it em st at us para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1880,0 +2649 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa m at ch es mo ve me nt ow ne r para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1890,0 +2660 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa m at ch es mo ve me nt ac co un t para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1900,0 +2671 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa m at ch es mo ve me nt te xt se ar ch para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1914,0 +2686 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa l ow er ca se para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1921,0 +2694 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa m on ey para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1928,0 +2702 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa f il te rm ov em en ti te ms fo rs im pl ep ag e para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1950,0 +2725 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa f il te rm ov em en ti te ms by te xt se ar ch para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1960,0 +2736 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa m at ch es mo ve me nt it em te xt se ar ch para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1974,0 +2751 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa c ou nt mo ve me nt it em s para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1985,0 +2763 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa c ou nt ex ac ts ta tu si te ms para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -1996,0 +2775 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa c ou nt ov er du ei te ms para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -2014,0 +2794,9 @@ public class PersonalFinanceController {
+	/**
+	 * Executa c re at ec ar dp ur ch as e.
+	 *
+	 * Mantem o contexto necessario da interface antes de devolver a resposta para a camada web.
+	 * @param form dados preenchidos no formulario da interface
+	 * @param bindingResult resultado acumulado das validações do formulario
+	 * @param redirectAttributes atributos temporarios usados no redirecionamento
+	 * @return nome da view Thymeleaf ou instrucao de redirecionamento gerada pelo fluxo
+	 */
@@ -2042 +2830 @@ public class PersonalFinanceController {
-			redirectAttributes.addFlashAttribute("successMessage", "Compra registrada com sucesso.");
+			redirectAttributes.addFlashAttribute("successMessage", "Compra registrada com sucesso!");
@@ -2049,0 +2838 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa r es ol ve pe ri od bo un ds para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -2059,0 +2849 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa r es ol ve mo nt hl yc on tr ol pe ri od bo un ds para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -2078,0 +2869 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa p ar se mo nt h para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -2089,0 +2881 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa h as te xt para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -2093,0 +2886 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa p ar se op ti on al mo nt h para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -2104,0 +2898 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa f or ma tm on th la be l para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -2112,0 +2907 @@ public class PersonalFinanceController {
+	// Centraliza a etapa de Executa c ap it al iz el ab el para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -2119,0 +2915,5 @@ public class PersonalFinanceController {
+/**
+ * Controller MVC responsável pelos fluxos de p er io db ou nd s.
+ *
+ * Exponibiliza endpoints web, prepara o Model usado pelos templatés Thymeleaf e encaminha as acoes da interface para a camada de serviço.
+ */
@@ -2122,0 +2923,5 @@ public class PersonalFinanceController {
+/**
+ * Controller MVC responsável pelos fluxos de m on th ly re po rt qu er y.
+ *
+ * Exponibiliza endpoints web, prepara o Model usado pelos templatés Thymeleaf e encaminha as acoes da interface para a camada de serviço.
+ */
@@ -2125,0 +2931,5 @@ public class PersonalFinanceController {
+/**
+ * Controller MVC responsável pelos fluxos de p er io dr ep or tq ue ry.
+ *
+ * Exponibiliza endpoints web, prepara o Model usado pelos templatés Thymeleaf e encaminha as acoes da interface para a camada de serviço.
+ */
@@ -2128,0 +2939,5 @@ public class PersonalFinanceController {
+/**
+ * Controller MVC responsável pelos fluxos de m on th ly re po rt co nt ex t.
+ *
+ * Exponibiliza endpoints web, prepara o Model usado pelos templatés Thymeleaf e encaminha as acoes da interface para a camada de serviço.
+ */
@@ -2131,0 +2947,5 @@ public class PersonalFinanceController {
+/**
+ * Controller MVC responsável pelos fluxos de p er io dr ep or tc on te xt.
+ *
+ * Exponibiliza endpoints web, prepara o Model usado pelos templatés Thymeleaf e encaminha as acoes da interface para a camada de serviço.
+ */
@@ -2134,0 +2955,5 @@ public class PersonalFinanceController {
+/**
+ * Controller MVC responsável pelos fluxos de c as hf lo wr ep or tc on te xt.
+ *
+ * Exponibiliza endpoints web, prepara o Model usado pelos templatés Thymeleaf e encaminha as acoes da interface para a camada de serviço.
+ */
@@ -2137,0 +2963,5 @@ public class PersonalFinanceController {
+/**
+ * Controller MVC responsável pelos fluxos de c at eg or yp er fo rm an ce re po rt co nt ex t.
+ *
+ * Exponibiliza endpoints web, prepara o Model usado pelos templatés Thymeleaf e encaminha as acoes da interface para a camada de serviço.
+ */
@@ -2140,0 +2971,5 @@ public class PersonalFinanceController {
+/**
+ * Controller MVC responsável pelos fluxos de r ep or tf il te rs el ec ti on.
+ *
+ * Exponibiliza endpoints web, prepara o Model usado pelos templatés Thymeleaf e encaminha as acoes da interface para a camada de serviço.
+ */
@@ -2145,0 +2981,5 @@ public class PersonalFinanceController {
+/**
+ * Controller MVC responsável pelos fluxos de m ov em en tf il te rs.
+ *
+ * Exponibiliza endpoints web, prepara o Model usado pelos templatés Thymeleaf e encaminha as acoes da interface para a camada de serviço.
+ */
@@ -2147,0 +2988 @@ public class PersonalFinanceController {
+		// Centraliza a etapa de Executa h as ac ti ve fi lt er s para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -2152,0 +2994 @@ public class PersonalFinanceController {
+		// Centraliza a etapa de Executa r es tr ic tm ov em en ts by pl an it em s para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -2158,0 +3001,5 @@ public class PersonalFinanceController {
+/**
+ * Controller MVC responsável pelos fluxos de m ov em en tm et ri cs.
+ *
+ * Exponibiliza endpoints web, prepara o Model usado pelos templatés Thymeleaf e encaminha as acoes da interface para a camada de serviço.
+ */
@@ -2164,0 +3012,5 @@ public class PersonalFinanceController {
+/**
+ * Controller MVC responsável pelos fluxos de f il te re dm ov em en ts re su lt.
+ *
+ * Exponibiliza endpoints web, prepara o Model usado pelos templatés Thymeleaf e encaminha as acoes da interface para a camada de serviço.
+ */
diff --git a/src/main/java/com/controle/finpessoal/controller/form/FinancialGoalForm.java b/src/main/java/com/controle/finpessoal/controller/form/FinancialGoalForm.java
index 97c980a..b03de59 100644
--- a/src/main/java/com/controle/finpessoal/controller/form/FinancialGoalForm.java
+++ b/src/main/java/com/controle/finpessoal/controller/form/FinancialGoalForm.java
@@ -14,0 +15,5 @@ import lombok.Setter;
+/**
+ * DTO de formulario usado para transportar dados de meta financeira entre a interface e os controllers.
+ *
+ * Ajuda a validar entradas vindas das telas e organiza os campos esperados em cada submissao.
+ */
@@ -21 +26 @@ public class FinancialGoalForm {
-	@NotBlank(message = "Nome da meta é obrigatório.")
+	@NotBlank(message = "O campo Nome da meta é obrigatório.")
@@ -24 +29 @@ public class FinancialGoalForm {
-	@NotNull(message = "Tipo da meta é obrigatório.")
+	@NotNull(message = "O campo Tipo da meta é obrigatório.")
@@ -27,2 +32,2 @@ public class FinancialGoalForm {
-	@NotNull(message = "Valor alvo é obrigatório.")
-	@DecimalMin(value = "0.01", message = "O valor alvo deve ser maior que zero.")
+	@NotNull(message = "O campo Valor alvo é obrigatório.")
+	@DecimalMin(value = "0.01", message = "O campo Valor alvo deve ser maior que zero.")
@@ -33 +38 @@ public class FinancialGoalForm {
-	@NotBlank(message = "Mês inicial é obrigatório.")
+	@NotBlank(message = "O campo Mês inicial é obrigatório.")
@@ -37,0 +43,6 @@ public class FinancialGoalForm {
+	/**
+	 * Executa command.
+	 *
+	 * Organiza esta etapa do fluxo para manter a leitura do código mais direta e reutilizável.
+	 * @return resultado produzido pela operação
+	 */
@@ -43,0 +55,7 @@ public class FinancialGoalForm {
+	/**
+	 * Executa mes.
+	 *
+	 * Organiza esta etapa do fluxo para manter a leitura do código mais direta e reutilizável.
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @return resultado produzido pela operação
+	 */
@@ -53,0 +72,7 @@ public class FinancialGoalForm {
+	/**
+	 * Executa savings meta.
+	 *
+	 * Organiza esta etapa do fluxo para manter a leitura do código mais direta e reutilizável.
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @return resultado produzido pela operação
+	 */
@@ -62,0 +88,7 @@ public class FinancialGoalForm {
+	/**
+	 * Executa catégoria limit.
+	 *
+	 * Organiza esta etapa do fluxo para manter a leitura do código mais direta e reutilizável.
+	 * @param monthValue mes de referência usado no fluxo atual
+	 * @return resultado produzido pela operação
+	 */
@@ -69,0 +102 @@ public class FinancialGoalForm {
+	// Centraliza a etapa de Converte mes valor para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -73 +106 @@ public class FinancialGoalForm {
-				throw new IllegalArgumentException(fieldLabel + " é obrigatório.");
+				throw new IllegalArgumentException("O campo " + fieldLabel + " é obrigatório.");
@@ -80 +113 @@ public class FinancialGoalForm {
-			throw new IllegalArgumentException(fieldLabel + " inválido.");
+			throw new IllegalArgumentException("O campo " + fieldLabel + " possui formato inválido.");
diff --git a/src/main/java/com/controle/finpessoal/service/FinanceiroDashboardService.java b/src/main/java/com/controle/finpessoal/service/FinanceiroDashboardService.java
index 810b35b..0b08e70 100644
--- a/src/main/java/com/controle/finpessoal/service/FinanceiroDashboardService.java
+++ b/src/main/java/com/controle/finpessoal/service/FinanceiroDashboardService.java
@@ -33,0 +34,5 @@ import org.springframework.transaction.annotation.Transactional;
+/**
+ * Serviço da camada de negócio responsável por painel financeiro.
+ *
+ * Centraliza validações, regras de negócio, orquestracao entre repositórios e preparacao de dados para os controllers e demais serviços.
+ */
@@ -48,0 +54,9 @@ public class FinanceiroDashboardService {
+	/**
+	 * Executa painel serviço.
+	 *
+	 * Aplica as regras da camada de negócio e orquestra os colaboradores envolvidos no fluxo.
+	 * @param personalFinanceMonthService mes de referência usado no fluxo atual
+	 * @param catégoryGroupRepository valor de apoio para catégoryGroupRepository
+	 * @param expensePaymentRepository valor de apoio para expensePaymentRepository
+	 * @param permissionValidator valor de apoio para permissionValidator
+	 */
@@ -58,0 +73,6 @@ public class FinanceiroDashboardService {
+	/**
+	 * Executa painel serviço.
+	 *
+	 * Aplica as regras da camada de negócio e orquestra os colaboradores envolvidos no fluxo.
+	 * @param personalFinanceMonthService mes de referência usado no fluxo atual
+	 */
@@ -62,0 +83,7 @@ public class FinanceiroDashboardService {
+	/**
+	 * Monta painel.
+	 *
+	 * Aplica as regras da camada de negócio e orquestra os colaboradores envolvidos no fluxo.
+	 * @param month mes de referência usado no fluxo atual
+	 * @return resultado produzido pela operação
+	 */
@@ -120,0 +148 @@ public class FinanceiroDashboardService {
+	// Centraliza a etapa de Monta totals para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -132,0 +161 @@ public class FinanceiroDashboardService {
+	// Centraliza a etapa de Monta composition chart para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -142,0 +172 @@ public class FinanceiroDashboardService {
+	// Centraliza a etapa de Monta tipo comparacao chart para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -151,0 +182 @@ public class FinanceiroDashboardService {
+	// Centraliza a etapa de Monta mensal evolution chart para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -170,0 +202 @@ public class FinanceiroDashboardService {
+	// Centraliza a etapa de Monta daily evolution chart para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -220,0 +253 @@ public class FinanceiroDashboardService {
+	// Centraliza a etapa de Monta despesa catégoria distribution para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -270,0 +304 @@ public class FinanceiroDashboardService {
+	// Centraliza a etapa de Monta mensal comparacao para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -285,0 +320 @@ public class FinanceiroDashboardService {
+	// Centraliza a etapa de Monta insights para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -296,0 +332 @@ public class FinanceiroDashboardService {
+	// Centraliza a etapa de Monta top catégoria insight para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -302 +338 @@ public class FinanceiroDashboardService {
-					"Nao houve despesas realizadas classificadas no mes selecionado.", "info", "bi-pie-chart",
+					"Não houve despesas realizadas classificadas no mês selecionado.", "info", "bi-pie-chart",
@@ -311,0 +348 @@ public class FinanceiroDashboardService {
+	// Centraliza a etapa de Monta despesa variation insight para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -321,2 +358,2 @@ public class FinanceiroDashboardService {
-			return new FinanceiroDashboardView.InsightCard("Despesas acima do mes anterior",
-					"As despesas realizadas cresceram " + roundedPercent(variation) + " em relacao a "
+			return new FinanceiroDashboardView.InsightCard("Despesas acima do mês anterior",
+					"As despesas realizadas cresceram " + roundedPercent(variation) + " em relação a "
@@ -328,2 +365,2 @@ public class FinanceiroDashboardService {
-			return new FinanceiroDashboardView.InsightCard("Despesas abaixo do mes anterior",
-					"As despesas realizadas cairam " + roundedPercent(Math.abs(variation)) + " em relacao a "
+			return new FinanceiroDashboardView.InsightCard("Despesas abaixo do mês anterior",
+					"As despesas realizadas caíram " + roundedPercent(Math.abs(variation)) + " em relação a "
@@ -334 +371 @@ public class FinanceiroDashboardService {
-		return new FinanceiroDashboardView.InsightCard("Despesas estaveis",
+		return new FinanceiroDashboardView.InsightCard("Despesas estáveis",
@@ -339,0 +377 @@ public class FinanceiroDashboardService {
+	// Centraliza a etapa de Monta alert insight para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -349 +387 @@ public class FinanceiroDashboardService {
-					"O saldo projetado do mes esta negativo em "
+					"O saldo projetado do mês está negativo em "
@@ -356,2 +394,2 @@ public class FinanceiroDashboardService {
-					"Existem " + overdueItems + " lancamentos em atraso no mes selecionado.", "warning",
-					"bi-exclamation-triangle", "Abrir movimentos",
+					"Existem " + overdueItems + " lançamentos em atraso no mês selecionado.", "warning",
+					"bi-exclamation-triangle", "Abrir movimentações",
@@ -362 +400 @@ public class FinanceiroDashboardService {
-					"As saidas pendentes somam " + MoneyUtils.formatAsCurrency(pendingOutflows) + " contra "
+					"As saídas pendentes somam " + MoneyUtils.formatAsCurrency(pendingOutflows) + " contra "
@@ -364 +402 @@ public class FinanceiroDashboardService {
-					"warning", "bi-hourglass-split", "Abrir movimentos",
+					"warning", "bi-hourglass-split", "Abrir movimentações",
@@ -369 +407 @@ public class FinanceiroDashboardService {
-						+ " e sem alertas criticos no mes.",
+						+ " e sem alertas críticos no mês.",
@@ -372,0 +411 @@ public class FinanceiroDashboardService {
+	// Centraliza a etapa de Soma restante by tipo para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -378,0 +418 @@ public class FinanceiroDashboardService {
+	// Centraliza a etapa de Conta overdue items para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -386,0 +427 @@ public class FinanceiroDashboardService {
+	// Centraliza a etapa de Resolve vencimento data para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -404,0 +446 @@ public class FinanceiroDashboardService {
+	// Centraliza a etapa de Executa items para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -408,0 +451 @@ public class FinanceiroDashboardService {
+	// Centraliza a etapa de Executa movimentos para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -412,0 +456 @@ public class FinanceiroDashboardService {
+	// Centraliza a etapa de Executa direct despesas para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -416,0 +461 @@ public class FinanceiroDashboardService {
+	// Centraliza a etapa de Executa direct despesas para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -421,0 +467 @@ public class FinanceiroDashboardService {
+	// Centraliza a etapa de Executa lista para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -429,0 +476 @@ public class FinanceiroDashboardService {
+	// Centraliza a etapa de Normaliza label para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -442,0 +490 @@ public class FinanceiroDashboardService {
+	// Centraliza a etapa de Resolve color para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -449,0 +498 @@ public class FinanceiroDashboardService {
+	// Centraliza a etapa de Resolve icon para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -453,0 +503 @@ public class FinanceiroDashboardService {
+	// Centraliza a etapa de Resolve grupo nome para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -457,0 +508 @@ public class FinanceiroDashboardService {
+	// Centraliza a etapa de Resolve classification para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -461,0 +513 @@ public class FinanceiroDashboardService {
+	// Centraliza a etapa de Executa percentage para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -469,0 +522 @@ public class FinanceiroDashboardService {
+	// Centraliza a etapa de Executa variation para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -479,0 +533 @@ public class FinanceiroDashboardService {
+	// Centraliza a etapa de Formata mes para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -484,0 +539 @@ public class FinanceiroDashboardService {
+	// Centraliza a etapa de Formata short mes para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -489,0 +545 @@ public class FinanceiroDashboardService {
+	// Centraliza a etapa de Executa capitalize para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -496,0 +553 @@ public class FinanceiroDashboardService {
+	// Centraliza a etapa de Executa percent para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -500,0 +558 @@ public class FinanceiroDashboardService {
+	// Centraliza a etapa de Executa money para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -513,0 +572 @@ public class FinanceiroDashboardService {
+		// Centraliza a etapa de Executa accumulator para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -523,0 +583 @@ public class FinanceiroDashboardService {
+		// Centraliza a etapa de Executa accumulator para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -528,0 +589 @@ public class FinanceiroDashboardService {
+		// Centraliza a etapa de Executa accumulaté para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -532,0 +594 @@ public class FinanceiroDashboardService {
+		// Centraliza a etapa de Executa grupo identificador para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -536,0 +599 @@ public class FinanceiroDashboardService {
+		// Centraliza a etapa de Executa label para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -540,0 +604 @@ public class FinanceiroDashboardService {
+		// Centraliza a etapa de Executa color para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -544,0 +609 @@ public class FinanceiroDashboardService {
+		// Centraliza a etapa de Executa icon para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -548,0 +614 @@ public class FinanceiroDashboardService {
+		// Centraliza a etapa de Executa nome para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -552,0 +619 @@ public class FinanceiroDashboardService {
+		// Centraliza a etapa de Executa classification para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -556,0 +624 @@ public class FinanceiroDashboardService {
+		// Centraliza a etapa de Executa amount para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
diff --git a/src/main/java/com/controle/finpessoal/service/GoalService.java b/src/main/java/com/controle/finpessoal/service/GoalService.java
index 937796a..b1009b7 100644
--- a/src/main/java/com/controle/finpessoal/service/GoalService.java
+++ b/src/main/java/com/controle/finpessoal/service/GoalService.java
@@ -18,0 +19,5 @@ import org.springframework.transaction.annotation.Transactional;
+/**
+ * Serviço da camada de negócio responsável por meta.
+ *
+ * Centraliza validações, regras de negócio, orquestracao entre repositórios e preparacao de dados para os controllers e demais serviços.
+ */
@@ -26,0 +32,7 @@ public class GoalService {
+	/**
+	 * Monta meta board.
+	 *
+	 * Aplica as regras da camada de negócio e orquestra os colaboradores envolvidos no fluxo.
+	 * @param month mes de referência usado no fluxo atual
+	 * @return resultado produzido pela operação
+	 */
@@ -91 +103 @@ public class GoalService {
-					? "Projecao suficiente para a meta"
+					? "Projeção suficiente para a meta"
@@ -111,0 +124 @@ public class GoalService {
+	// Centraliza a etapa de Executa only para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -115,0 +129 @@ public class GoalService {
+	// Centraliza a etapa de Executa percentage para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -123,0 +138 @@ public class GoalService {
+	// Centraliza a etapa de Executa money para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -127,0 +143 @@ public class GoalService {
+	// Centraliza a etapa de Formata deadline para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -135,0 +152,5 @@ public class GoalService {
+/**
+ * Serviço da camada de negócio responsável por meta board.
+ *
+ * Centraliza validações, regras de negócio, orquestracao entre repositórios e preparacao de dados para os controllers e demais serviços.
+ */
@@ -138,0 +160,6 @@ public class GoalService {
+		/**
+		 * Executa metas.
+		 *
+		 * Aplica as regras da camada de negócio e orquestra os colaboradores envolvidos no fluxo.
+		 * @return lista resultante da operação
+		 */
@@ -145,0 +173,6 @@ public class GoalService {
+		/**
+		 * Executa savings meta quantidade.
+		 *
+		 * Aplica as regras da camada de negócio e orquestra os colaboradores envolvidos no fluxo.
+		 * @return resultado produzido pela operação
+		 */
@@ -149,0 +183,6 @@ public class GoalService {
+		/**
+		 * Executa savings gap.
+		 *
+		 * Aplica as regras da camada de negócio e orquestra os colaboradores envolvidos no fluxo.
+		 * @return resultado produzido pela operação
+		 */
@@ -155,0 +195,6 @@ public class GoalService {
+		/**
+		 * Executa projecao disclaimer.
+		 *
+		 * Aplica as regras da camada de negócio e orquestra os colaboradores envolvidos no fluxo.
+		 * @return resultado produzido pela operação
+		 */
@@ -157 +202 @@ public class GoalService {
-			return "As metas de projeto usam economia e resultado projetados do mes. O sistema ainda nao controla aporte reservado em conta.";
+			return "As metas de projeto usam economia e resultado projetados do mês. O sistema ainda não controla aporte reservado em conta.";
@@ -160,0 +206,5 @@ public class GoalService {
+/**
+ * Serviço da camada de negócio responsável por meta progress.
+ *
+ * Centraliza validações, regras de negócio, orquestracao entre repositórios e preparacao de dados para os controllers e demais serviços.
+ */
@@ -165,0 +216,5 @@ public class GoalService {
+/**
+ * Serviço da camada de negócio responsável por budget progress.
+ *
+ * Centraliza validações, regras de negócio, orquestracao entre repositórios e preparacao de dados para os controllers e demais serviços.
+ */
diff --git a/src/main/java/com/controle/finpessoal/service/NotificationService.java b/src/main/java/com/controle/finpessoal/service/NotificationService.java
index 85924c4..172ffe2 100644
--- a/src/main/java/com/controle/finpessoal/service/NotificationService.java
+++ b/src/main/java/com/controle/finpessoal/service/NotificationService.java
@@ -21,0 +22,5 @@ import org.springframework.transaction.annotation.Transactional;
+/**
+ * Serviço da camada de negócio responsável por notificacao.
+ *
+ * Centraliza validações, regras de negócio, orquestracao entre repositórios e preparacao de dados para os controllers e demais serviços.
+ */
@@ -32,0 +38,7 @@ public class NotificationService {
+	/**
+	 * Monta notifications.
+	 *
+	 * Aplica as regras da camada de negócio e orquestra os colaboradores envolvidos no fluxo.
+	 * @param month mes de referência usado no fluxo atual
+	 * @return lista resultante da operação
+	 */
@@ -112,2 +124,2 @@ public class NotificationService {
-					notifications.add(new NotificationView("Projecao abaixo da meta",
-							goal.name() + " ainda nao tem economia projetada suficiente para o mes.", "warning",
+					notifications.add(new NotificationView("Projeção abaixo da meta",
+							goal.name() + " ainda não tem economia projetada suficiente para o mês.", "warning",
@@ -136,0 +149 @@ public class NotificationService {
+	// Centraliza a etapa de Resolve vencimento data para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -146,0 +160 @@ public class NotificationService {
+	// Centraliza a etapa de Formata percent para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -150,0 +165,5 @@ public class NotificationService {
+/**
+ * Serviço da camada de negócio responsável por notificacao.
+ *
+ * Centraliza validações, regras de negócio, orquestracao entre repositórios e preparacao de dados para os controllers e demais serviços.
+ */
diff --git a/src/main/java/com/controle/finpessoal/service/PersonalFinanceReportExportService.java b/src/main/java/com/controle/finpessoal/service/PersonalFinanceReportExportService.java
index 7f0b017..9718dbe 100644
--- a/src/main/java/com/controle/finpessoal/service/PersonalFinanceReportExportService.java
+++ b/src/main/java/com/controle/finpessoal/service/PersonalFinanceReportExportService.java
@@ -43,0 +44,5 @@ import org.springframework.stereotype.Service;
+/**
+ * Serviço da camada de negócio responsável por pessoal financeiro relatório exportacao.
+ *
+ * Centraliza validações, regras de negócio, orquestracao entre repositórios e preparacao de dados para os controllers e demais serviços.
+ */
@@ -61,0 +67,8 @@ public class PersonalFinanceReportExportService {
+	/**
+	 * Exporta mensal relatório pdf.
+	 *
+	 * Aplica as regras da camada de negócio e orquestra os colaboradores envolvidos no fluxo.
+	 * @param report valor de apoio para report
+	 * @param subtitle valor de apoio para subtitle
+	 * @return resultado produzido pela operação
+	 */
@@ -70,0 +84,8 @@ public class PersonalFinanceReportExportService {
+	/**
+	 * Exporta mensal relatório excel.
+	 *
+	 * Aplica as regras da camada de negócio e orquestra os colaboradores envolvidos no fluxo.
+	 * @param report valor de apoio para report
+	 * @param subtitle valor de apoio para subtitle
+	 * @return resultado produzido pela operação
+	 */
@@ -79,0 +101,8 @@ public class PersonalFinanceReportExportService {
+	/**
+	 * Exporta período relatório pdf.
+	 *
+	 * Aplica as regras da camada de negócio e orquestra os colaboradores envolvidos no fluxo.
+	 * @param report valor de apoio para report
+	 * @param subtitle valor de apoio para subtitle
+	 * @return resultado produzido pela operação
+	 */
@@ -88,0 +118,8 @@ public class PersonalFinanceReportExportService {
+	/**
+	 * Exporta período relatório excel.
+	 *
+	 * Aplica as regras da camada de negócio e orquestra os colaboradores envolvidos no fluxo.
+	 * @param report valor de apoio para report
+	 * @param subtitle valor de apoio para subtitle
+	 * @return resultado produzido pela operação
+	 */
@@ -97,0 +135,8 @@ public class PersonalFinanceReportExportService {
+	/**
+	 * Exporta caixa fluxo pdf.
+	 *
+	 * Aplica as regras da camada de negócio e orquestra os colaboradores envolvidos no fluxo.
+	 * @param report valor de apoio para report
+	 * @param subtitle valor de apoio para subtitle
+	 * @return resultado produzido pela operação
+	 */
@@ -104,0 +150,8 @@ public class PersonalFinanceReportExportService {
+	/**
+	 * Exporta caixa fluxo excel.
+	 *
+	 * Aplica as regras da camada de negócio e orquestra os colaboradores envolvidos no fluxo.
+	 * @param report valor de apoio para report
+	 * @param subtitle valor de apoio para subtitle
+	 * @return resultado produzido pela operação
+	 */
@@ -111,0 +165,8 @@ public class PersonalFinanceReportExportService {
+	/**
+	 * Exporta catégoria desempenho pdf.
+	 *
+	 * Aplica as regras da camada de negócio e orquestra os colaboradores envolvidos no fluxo.
+	 * @param report valor de apoio para report
+	 * @param subtitle valor de apoio para subtitle
+	 * @return resultado produzido pela operação
+	 */
@@ -118,0 +180,8 @@ public class PersonalFinanceReportExportService {
+	/**
+	 * Exporta catégoria desempenho excel.
+	 *
+	 * Aplica as regras da camada de negócio e orquestra os colaboradores envolvidos no fluxo.
+	 * @param report valor de apoio para report
+	 * @param subtitle valor de apoio para subtitle
+	 * @return resultado produzido pela operação
+	 */
@@ -125,0 +195,8 @@ public class PersonalFinanceReportExportService {
+	/**
+	 * Exporta planejámento items pdf.
+	 *
+	 * Aplica as regras da camada de negócio e orquestra os colaboradores envolvidos no fluxo.
+	 * @param plan valor de apoio para plan
+	 * @param subtitle valor de apoio para subtitle
+	 * @return resultado produzido pela operação
+	 */
@@ -132,0 +210,8 @@ public class PersonalFinanceReportExportService {
+	/**
+	 * Exporta planejámento items excel.
+	 *
+	 * Aplica as regras da camada de negócio e orquestra os colaboradores envolvidos no fluxo.
+	 * @param plan valor de apoio para plan
+	 * @param subtitle valor de apoio para subtitle
+	 * @return resultado produzido pela operação
+	 */
@@ -139,0 +225 @@ public class PersonalFinanceReportExportService {
+	// Centraliza a etapa de Cria pdf para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -206,0 +293 @@ public class PersonalFinanceReportExportService {
+	// Centraliza a etapa de Cria cabecalho cell para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -216,0 +304 @@ public class PersonalFinanceReportExportService {
+	// Centraliza a etapa de Cria body cell para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -226,0 +315 @@ public class PersonalFinanceReportExportService {
+	// Centraliza a etapa de Cria empty cell para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -237,0 +327 @@ public class PersonalFinanceReportExportService {
+	// Centraliza a etapa de Cria excel para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -322,0 +413 @@ public class PersonalFinanceReportExportService {
+	// Centraliza a etapa de Executa single line para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -332,0 +424 @@ public class PersonalFinanceReportExportService {
+	// Centraliza a etapa de Monta mensal resumo section para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -343,2 +435,2 @@ public class PersonalFinanceReportExportService {
-						List.of("Total pago em movimentos", moneyLabel(report.totalPaidInMovements())),
-						List.of("Média por movimento", moneyLabel(report.averageMovementValue()))));
+				List.of("Total pago em movimentações", moneyLabel(report.totalPaidInMovements())),
+				List.of("Média por movimentação", moneyLabel(report.averageMovementValue()))));
@@ -346,0 +439 @@ public class PersonalFinanceReportExportService {
+	// Centraliza a etapa de Monta mensal status section para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -355,0 +449 @@ public class PersonalFinanceReportExportService {
+	// Centraliza a etapa de Monta mensal grupos section para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -364,0 +459 @@ public class PersonalFinanceReportExportService {
+	// Centraliza a etapa de Monta período grupos section para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -373,0 +469 @@ public class PersonalFinanceReportExportService {
+	// Centraliza a etapa de Monta mensal contas section para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -377 +473 @@ public class PersonalFinanceReportExportService {
-		return new ExportSection("Pagamentos por conta", List.of("Conta", "Movimentos", "Total pago"), rows);
+		return new ExportSection("Pagamentos por conta", List.of("Conta", "Movimentações", "Total pago"), rows);
@@ -379,0 +476 @@ public class PersonalFinanceReportExportService {
+	// Centraliza a etapa de Monta período mensal section para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -386 +483 @@ public class PersonalFinanceReportExportService {
-				List.of("Mês", "Entradas", "Despesas", "Investimentos", "Saldo", "Itens", "Movimentos"), rows);
+				List.of("Mês", "Entradas", "Despesas", "Investimentos", "Saldo", "Itens", "Movimentações"), rows);
@@ -388,0 +486 @@ public class PersonalFinanceReportExportService {
+	// Centraliza a etapa de Monta período totals section para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -397,0 +496 @@ public class PersonalFinanceReportExportService {
+	// Centraliza a etapa de Monta caixa fluxo resumo section para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -404,0 +504 @@ public class PersonalFinanceReportExportService {
+	// Centraliza a etapa de Monta caixa fluxo points section para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -413 +513 @@ public class PersonalFinanceReportExportService {
-				List.of("Mês", "Entradas", "Saídas", "Fluxo líquido", "Acumulado", "Itens", "Movimentos"), rows);
+				List.of("Mês", "Entradas", "Saídas", "Fluxo líquido", "Acumulado", "Itens", "Movimentações"), rows);
@@ -415,0 +516 @@ public class PersonalFinanceReportExportService {
+	// Centraliza a etapa de Monta catégoria resumo section para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -423,0 +525 @@ public class PersonalFinanceReportExportService {
+	// Centraliza a etapa de Monta catégoria rows section para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -434,0 +537 @@ public class PersonalFinanceReportExportService {
+	// Centraliza a etapa de Monta planejámento items resumo section para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -471,0 +575 @@ public class PersonalFinanceReportExportService {
+	// Centraliza a etapa de Monta planejámento items rows section para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -483,0 +588 @@ public class PersonalFinanceReportExportService {
+	// Centraliza a etapa de Formata planejámento item description para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -498,0 +604 @@ public class PersonalFinanceReportExportService {
+	// Centraliza a etapa de Executa label para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -502,0 +609 @@ public class PersonalFinanceReportExportService {
+	// Centraliza a etapa de Executa label para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -507,0 +615 @@ public class PersonalFinanceReportExportService {
+	// Centraliza a etapa de Executa label para manter o fluxo principal mais legível e reaproveitar a mesma regra neste arquivo.
@@ -514,0 +623,5 @@ public class PersonalFinanceReportExportService {
+/**
+ * Serviço da camada de negócio responsável por exportacao section.
+ *
+ * Centraliza validações, regras de negócio, orquestracao entre repositórios e preparacao de dados para os controllers e demais serviços.
+ */
diff --git a/src/main/resources/application.properties b/src/main/resources/application.properties
index f64c4e9..bb20279 100644
--- a/src/main/resources/application.properties
+++ b/src/main/resources/application.properties
@@ -1,0 +2,3 @@
+# Conexão principal do sistema.
+# Os valores podem ser sobrescritos por variáveis de ambiente para facilitar
+# execução local, containerização e deploy sem editar este arquivo.
@@ -5,0 +9,2 @@ spring.datasource.driver-class-name=org.postgresql.Driver
+# Pool Hikari: mantém conexões prontas para reduzir latência e controlar
+# consumo de recursos do banco conforme a carga do sistema.
@@ -16,0 +22,2 @@ spring.datasource.hikari.data-source-properties.reWriteBatchedInserts=${SPRING_D
+# O schema é validado contra as entidades.
+# Alterações estruturais devem entrar por migration do Flyway, não por ddl-auto.
@@ -19,0 +27 @@ spring.jpa.properties.hibernate.format_sql=true
+# Batch e ordenação ajudam a reduzir round-trips em operações de escrita maiores.
@@ -24,0 +33 @@ spring.jpa.properties.hibernate.order_updates=true
+# Migrations SQL versionadas que mantêm o banco sincronizado com a aplicação.
@@ -29,0 +39 @@ spring.flyway.validate-on-migrate=true
+# Em ambiente local mantemos cache desligado para refletir alterações de tela sem restart completo.
@@ -32,0 +43 @@ spring.thymeleaf.cache=${SPRING_THYMELEAF_CACHE:false}
+# Padroniza exibição em pt-BR nas datas e horários enviados para a interface.
@@ -39,0 +51 @@ spring.mvc.format.date-time=dd/MM/yyyy HH:mm:ss
+# Porta, comportamento de erro e política de sessão usada pelo Spring Boot.
@@ -54,0 +67,2 @@ server.compression.mime-types=text/html,text/xml,text/plain,text/css,text/javasc
+# Configurações internas do módulo financeiro: URL base, email remetente e
+# opções de bootstrap do acesso administrativo e remember-me.
@@ -68,0 +83 @@ app.security.remember-me.always=${APP_SECURITY_REMEMBER_ME_ALWAYS:false}
+# Usado por fluxos de email como recuperação de senha e notificações futuras.
diff --git a/src/main/resources/static/js/page-search.js b/src/main/resources/static/js/page-search.js
index f915c93..74ea498 100644
--- a/src/main/resources/static/js/page-search.js
+++ b/src/main/resources/static/js/page-search.js
@@ -151 +151 @@
-        state.status.textContent = message || "Digite para localizar qualquer informacao exibida nesta pagina.";
+        state.status.textContent = message || "Digite para localizar qualquer informação exibida nesta página.";
@@ -162 +162 @@
-            setStatus(state, "Digite para localizar qualquer informacao exibida nesta pagina.");
+            setStatus(state, "Digite para localizar qualquer informação exibida nesta página.");
diff --git a/src/main/resources/templates/auth/login.html b/src/main/resources/templates/auth/login.html
index d952070..0eb5b7c 100644
--- a/src/main/resources/templates/auth/login.html
+++ b/src/main/resources/templates/auth/login.html
@@ -1,0 +2,6 @@
+<!--
+  TELA: Login
+  ARQUIVO: src\main\resources\templatés\auth\login.html
+  TIPO: Template Thymeleaf
+  OBJETIVO: Documenta a função deste template dentro da interface web.
+-->
@@ -315 +321 @@
-                               placeholder="admin ou voce@dominio" autofocus required/>
+                               placeholder="admin ou usuario@dominio.com.br" autofocus required/>
diff --git a/src/main/resources/templates/dashboard/calendar.html b/src/main/resources/templates/dashboard/calendar.html
index 8e062a6..281843a 100644
--- a/src/main/resources/templates/dashboard/calendar.html
+++ b/src/main/resources/templates/dashboard/calendar.html
@@ -1,0 +2,6 @@
+<!--
+  TELA: Calendar
+  ARQUIVO: src\main\resources\templatés\dashboard\calendar.html
+  TIPO: Template Thymeleaf
+  OBJETIVO: Documenta a função deste template dentro da interface web.
+-->
@@ -15 +21 @@
-                <span>Ver movimentos</span>
+                <span>Ver movimentações</span>
@@ -78 +84 @@
-                                       th:text="|+ ${#lists.size(day.entries()) - 3} itens|">+ 1 item</small>
+                                       th:text="|+ ${#lists.size(day.entries()) - 3} ${#lists.size(day.entries()) - 3 == 1 ? 'item' : 'itens'}|">+ 1 item</small>
diff --git a/src/main/resources/templates/error.html b/src/main/resources/templates/error.html
index ec64a63..65abee4 100644
--- a/src/main/resources/templates/error.html
+++ b/src/main/resources/templates/error.html
@@ -1,0 +2,6 @@
+<!--
+  TELA: Error
+  ARQUIVO: src\main\resources\templatés\error.html
+  TIPO: Template Thymeleaf
+  OBJETIVO: Documenta a função deste template dentro da interface web.
+-->
@@ -58,2 +64,2 @@
-                        <a th:href="@{/}" class="btn btn-primary">Voltar ao inicio</a>
-                        <a th:href="@{/login}" class="btn btn-outline-secondary">Ir para login</a>
+                        <a th:href="@{/}" class="btn btn-primary">Voltar ao início</a>
+                        <a th:href="@{/login}" class="btn btn-outline-secondary">Ir para o login</a>
diff --git a/src/main/resources/templates/financeiro-pessoal/cadastros-cartoes.html b/src/main/resources/templates/financeiro-pessoal/cadastros-cartoes.html
index c319686..2965b16 100644
--- a/src/main/resources/templates/financeiro-pessoal/cadastros-cartoes.html
+++ b/src/main/resources/templates/financeiro-pessoal/cadastros-cartoes.html
@@ -1,0 +2,8 @@
+<!--
+  TELA: Gestão de Cartões
+  ROTA: /financeiro-pessoal/cartoes?mes=YYYY-MM
+  CONTROLLER: CreditCardController
+
+  Gerencia cartoes, compras e configurações do ciclo de fatura
+  usadas pelos lancamentos vinculados a cartao de crédito.
+-->
@@ -92,0 +101,8 @@
+    <nav class="pf-breadcrumb" aria-label="Breadcrumb">
+        <a class="pf-breadcrumb__link" th:href="@{/financeiro-pessoal(mes=${selectedMonthValue})}">Painel</a>
+        <span class="pf-breadcrumb__separator">›</span>
+        <a class="pf-breadcrumb__link" th:href="@{/financeiro-pessoal/cadastros/usuarios(mes=${selectedMonthValue})}">Cadastros</a>
+        <span class="pf-breadcrumb__separator">›</span>
+        <span class="pf-breadcrumb__current">Cartões</span>
+    </nav>
+
@@ -98,0 +115,16 @@
+    <div class="pf-tip-card" data-pf-tip-card>
+        <div class="pf-tip-card__header">
+            <span class="pf-tip-card__title">
+                <i class="bi bi-lightbulb"></i>
+                <span>Fechamento x vencimento</span>
+            </span>
+            <button type="button" class="pf-tip-card__close" data-pf-tip-close aria-label="Fechar dica">
+                <i class="bi bi-x-lg"></i>
+            </button>
+        </div>
+        <p>
+            O dia de <strong>fechamento</strong> define em qual fatura a compra entra. O dia de <strong>vencimento</strong>
+            define quando aquela fatura precisa ser paga. Configure isso com cuidado para o sistema calcular o ciclo correto.
+        </p>
+    </div>
+
@@ -124 +156 @@
-                                    <input id="newCardName" type="text" class="form-control" th:field="*{name}" placeholder="Ex: Visa Platinum" required>
+                                    <input id="newCardName" type="text" class="form-control" th:field="*{name}" placeholder="Ex: Nubank Mastercard, Inter Visa" required>
@@ -133 +165 @@
-                                    <input id="newCardBank" type="text" class="form-control" th:field="*{bank}" placeholder="Ex: Itaú">
+                                    <input id="newCardBank" type="text" class="form-control" th:field="*{bank}" placeholder="Ex: Itaú, Nubank, Inter">
@@ -137 +169,7 @@
-                                    <label class="form-label" for="newCardClosingDay">Fechamento</label>
+                                    <label class="form-label" for="newCardClosingDay">
+                                        Fechamento
+                                        <span class="pf-tooltip-icon"
+                                              title="Compras feitas depois desse dia vão para a próxima fatura.">
+                                            <i class="bi bi-question-circle"></i>
+                                        </span>
+                                    </label>
@@ -138,0 +177 @@
+                                    <small class="pf-form-help">Ex: se fecha dia 10, compras no dia 11 já entram na fatura seguinte.</small>
@@ -141 +180,7 @@
-                                    <label class="form-label" for="newCardDueDay">Vencimento</label>
+                                    <label class="form-label" for="newCardDueDay">
+                                        Vencimento
+                                        <span class="pf-tooltip-icon"
+                                              title="Dia em que a fatura precisa ser paga para evitar atraso.">
+                                            <i class="bi bi-question-circle"></i>
+                                        </span>
+                                    </label>
@@ -146 +191 @@
-                                    <input id="newCardLimit" type="number" step="0.01" class="form-control" th:field="*{creditLimit}">
+                                    <input id="newCardLimit" type="number" step="0.01" class="form-control" th:field="*{creditLimit}" placeholder="Ex: 3500,00">
@@ -160 +205,7 @@
-                                    <label class="form-label" for="newCardDefaultAccount">Conta padrão</label>
+                                    <label class="form-label" for="newCardDefaultAccount">
+                                        Conta padrão
+                                        <span class="pf-tooltip-icon"
+                                              title="Conta sugerida quando a fatura for paga ou quando a compra gerar baixa vinculada.">
+                                            <i class="bi bi-question-circle"></i>
+                                        </span>
+                                    </label>
@@ -170 +221 @@
-                                              placeholder="Informações opcionais"></textarea>
+                                              placeholder="Opcional — ex: Cartão usado só para assinatura e viagem"></textarea>
@@ -222 +273 @@
-                                <a th:href="@{/financeiro-pessoal/cartoes/{id}(id=${card.id}, mes=${cardStatementMonths[card.id]})}" class="btn btn-sm btn-outline-primary">Ver Faturas</a>
+                                <a th:href="@{/financeiro-pessoal/cartoes/{id}(id=${card.id}, mes=${cardStatementMonths[card.id]})}" class="btn btn-sm btn-outline-primary">Ver faturas</a>
@@ -244 +295 @@
-                                          onsubmit="return confirm('Excluir este cartão permanentemente?');">
+                                          onsubmit="return confirm('Excluir este cartão permanentemente? Use essa opção apenas se ele foi cadastrado por engano.');">
@@ -252 +303 @@
-                                          onsubmit="return confirm('Inativar este cartão?');">
+                                          onsubmit="return confirm('Inativar este cartão? Ele deixa de aparecer em novos lançamentos, mas o histórico de compras continua salvo.');">
diff --git a/src/main/resources/templates/financeiro-pessoal/cadastros-contas.html b/src/main/resources/templates/financeiro-pessoal/cadastros-contas.html
index e14fc01..f9710bc 100644
--- a/src/main/resources/templates/financeiro-pessoal/cadastros-contas.html
+++ b/src/main/resources/templates/financeiro-pessoal/cadastros-contas.html
@@ -1,0 +2,6 @@
+<!--
+  TELA: Cadastros Contas
+  ARQUIVO: src\main\resources\templatés\financeiro-pessoal\cadastros-contas.html
+  TIPO: Template Thymeleaf
+  OBJETIVO: Permite cadastrar, atualizar e controlar o status das contas usadas no sistema.
+-->
@@ -8,0 +15,8 @@
+    <nav class="pf-breadcrumb" aria-label="Breadcrumb">
+        <a class="pf-breadcrumb__link" th:href="@{/financeiro-pessoal(mes=${selectedMonthValue})}">Painel</a>
+        <span class="pf-breadcrumb__separator">›</span>
+        <a class="pf-breadcrumb__link" th:href="@{/financeiro-pessoal/cadastros/usuarios(mes=${selectedMonthValue})}">Cadastros</a>
+        <span class="pf-breadcrumb__separator">›</span>
+        <span class="pf-breadcrumb__current">Contas</span>
+    </nav>
+
@@ -14,0 +29,16 @@
+    <div class="pf-tip-card" data-pf-tip-card>
+        <div class="pf-tip-card__header">
+            <span class="pf-tip-card__title">
+                <i class="bi bi-lightbulb"></i>
+                <span>Quando criar uma conta</span>
+            </span>
+            <button type="button" class="pf-tip-card__close" data-pf-tip-close aria-label="Fechar dica">
+                <i class="bi bi-x-lg"></i>
+            </button>
+        </div>
+        <p>
+            Cadastre aqui tudo o que funciona como origem ou destino do seu dinheiro: banco, carteira e contas de investimento.
+            Isso deixa os pagamentos e recebimentos muito mais claros nas telas de movimentações e relatórios.
+        </p>
+    </div>
+
@@ -30 +60,2 @@
-                                <input type="text" class="form-control" th:field="*{name}" maxlength="120" required>
+                                <input type="text" class="form-control" th:field="*{name}" maxlength="120" placeholder="Ex: Nubank, Itaú Corrente, Carteira" required>
+                                <small class="pf-form-help">Use um nome reconhecível para achar a conta rápido nos filtros e lançamentos.</small>
@@ -33 +64,7 @@
-                                <label class="form-label">Tipo</label>
+                                <label class="form-label">
+                                    Tipo
+                                    <span class="pf-tooltip-icon"
+                                          title="Escolha se a conta representa banco, dinheiro em mãos ou investimento.">
+                                        <i class="bi bi-question-circle"></i>
+                                    </span>
+                                </label>
@@ -40 +77,7 @@
-                                <label class="form-label">Usuário dono da conta</label>
+                                <label class="form-label">
+                                    Usuário dono da conta
+                                    <span class="pf-tooltip-icon"
+                                          title="Define de quem é a conta quando o sistema for compartilhado por mais de uma pessoa.">
+                                        <i class="bi bi-question-circle"></i>
+                                    </span>
+                                </label>
@@ -44,0 +88 @@
+                                <small class="pf-form-help">Esse vínculo ajuda a filtrar relatórios e responsabilidades por pessoa.</small>
@@ -142 +186 @@
-                                              onsubmit="return confirm('Inativar esta conta?');">
+                                              onsubmit="return confirm('Inativar esta conta? Ela deixa de aparecer como opcao nova, mas o histórico ligado a ela continua salvo.');">
@@ -164 +208,3 @@
-                                <td colspan="5" class="text-center text-muted py-4">Nenhuma conta cadastrada.</td>
+                                <td colspan="5" class="py-4">
+                                    <th:block th:replace="~{fragments/redesign :: emptyState('bi-bank', 'Nenhuma conta cadastrada', 'Cadastre sua primeira conta para registrar de onde o dinheiro sai e para onde ele vai.', null, null)}"></th:block>
+                                </td>
diff --git a/src/main/resources/templates/financeiro-pessoal/cadastros-grupos.html b/src/main/resources/templates/financeiro-pessoal/cadastros-grupos.html
index f7bf0e9..5de568c 100644
--- a/src/main/resources/templates/financeiro-pessoal/cadastros-grupos.html
+++ b/src/main/resources/templates/financeiro-pessoal/cadastros-grupos.html
@@ -1,0 +2,8 @@
+<!--
+  TELA: Cadastro de Classificações e Subclassificações
+  ROTA: /financeiro-pessoal/cadastros/grupos ou /financeiro-pessoal/cadastros/subclassificações
+  CONTROLLER: PersonalFinanceController#cadastroGrupos() / #cadastroSubclassificações()
+
+  Organiza a estrutura que classifica receitas, despesas e investimentos
+  nas demais telas do sistema.
+-->
@@ -9,0 +18,8 @@
+    <nav class="pf-breadcrumb" aria-label="Breadcrumb">
+        <a class="pf-breadcrumb__link" th:href="@{/financeiro-pessoal(mes=${selectedMonthValue})}">Painel</a>
+        <span class="pf-breadcrumb__separator">›</span>
+        <a class="pf-breadcrumb__link" th:href="@{/financeiro-pessoal/cadastros/usuarios(mes=${selectedMonthValue})}">Cadastros</a>
+        <span class="pf-breadcrumb__separator">›</span>
+        <span class="pf-breadcrumb__current" th:text="${isSubclassificationCatalog ? 'Subclassificações' : 'Classificações'}">Classificações</span>
+    </nav>
+
@@ -15,0 +32,16 @@
+    <div class="pf-tip-card" data-pf-tip-card>
+        <div class="pf-tip-card__header">
+            <span class="pf-tip-card__title">
+                <i class="bi bi-lightbulb"></i>
+                <span>Dica de organização</span>
+            </span>
+            <button type="button" class="pf-tip-card__close" data-pf-tip-close aria-label="Fechar dica">
+                <i class="bi bi-x-lg"></i>
+            </button>
+        </div>
+        <p>
+            Use <strong>classificações</strong> para os grandes blocos do seu orçamento, como Moradia e Alimentação.
+            Use <strong>subclassificações</strong> para detalhar melhor cada bloco quando isso fizer sentido para você.
+        </p>
+    </div>
+
@@ -49 +81,2 @@
-                                <input type="text" class="form-control" th:field="*{name}" maxlength="120" required>
+                                <input type="text" class="form-control" th:field="*{name}" maxlength="120" placeholder="Ex: Moradia, Alimentação, Lazer" required>
+                                <small class="pf-form-help">Escolha um nome simples e direto para facilitar filtros, relatórios e leitura do dashboard.</small>
@@ -53 +86,7 @@
-                                <label class="form-label">Tipo</label>
+                                <label class="form-label">
+                                    Tipo
+                                    <span class="pf-tooltip-icon"
+                                          title="Define se a classificação entra como receita, despesa ou investimento.">
+                                        <i class="bi bi-question-circle"></i>
+                                    </span>
+                                </label>
@@ -57,0 +97 @@
+                                <small class="pf-form-help">Esse tipo influencia saldo, indicadores e regras de leitura dos relatórios.</small>
@@ -203 +243 @@
-                                              onsubmit="return confirm('Inativar esta classificação?');">
+                                              onsubmit="return confirm('Inativar esta classificação? Ela deixa de aparecer para novos lançamentos, mas o histórico já registrado continua preservado.');">
@@ -230,2 +270,2 @@
-                                    th:text="${isSubclassificationCatalog ? 'Nenhuma subclassificação cadastrada.' : 'Nenhuma classificação cadastrada.'}">
-                                    Nenhuma classificação cadastrada.
+                                    class="py-4">
+                                    <th:block th:replace="~{fragments/redesign :: emptyState('bi-diagram-3', ${isSubclassificationCatalog ? 'Nenhuma subclassificação cadastrada' : 'Nenhuma classificação cadastrada'}, ${isSubclassificationCatalog ? 'Crie uma subclassificação para detalhar melhor suas despesas e receitas.' : 'Crie sua primeira classificação para organizar o planejamento e os relatórios.'}, null, null)}"></th:block>
diff --git a/src/main/resources/templates/financeiro-pessoal/despesas/pagamentos/form.html b/src/main/resources/templates/financeiro-pessoal/despesas/pagamentos/form.html
index 9d9276f..f5114e0 100644
--- a/src/main/resources/templates/financeiro-pessoal/despesas/pagamentos/form.html
+++ b/src/main/resources/templates/financeiro-pessoal/despesas/pagamentos/form.html
@@ -1,0 +2,8 @@
+<!--
+  TELA: Formulário de Pagamento Avulso
+  ROTA: /financeiro-pessoal/despesas/pagamentos/novo ou /{id}/editar
+  CONTROLLER: ExpensePaymentController
+
+  Cadastra e ajusta pagamentos de despesas que precisam ser controlados
+  fora do fluxo principal de lancamentos planejádos.
+-->
@@ -8,0 +17,8 @@
+    <nav class="pf-breadcrumb" aria-label="Breadcrumb">
+        <a class="pf-breadcrumb__link" th:href="@{/financeiro-pessoal(mes=${selectedMonthValue})}">Painel</a>
+        <span class="pf-breadcrumb__separator">›</span>
+        <a class="pf-breadcrumb__link" th:href="@{/financeiro-pessoal/despesas/pagamentos(mes=${selectedMonthValue})}">Pagamentos</a>
+        <span class="pf-breadcrumb__separator">›</span>
+        <span class="pf-breadcrumb__current" th:text="${isNew} ? 'Novo pagamento' : 'Editar pagamento'">Pagamento</span>
+    </nav>
+
@@ -21,0 +38,16 @@
+    <div class="pf-tip-card" data-pf-tip-card>
+        <div class="pf-tip-card__header">
+            <span class="pf-tip-card__title">
+                <i class="bi bi-lightbulb"></i>
+                <span>Quando usar pagamento avulso</span>
+            </span>
+            <button type="button" class="pf-tip-card__close" data-pf-tip-close aria-label="Fechar dica">
+                <i class="bi bi-x-lg"></i>
+            </button>
+        </div>
+        <p>
+            Use esta tela para despesas que você quer registrar direto como pagamento, sem criar antes um item no planejamento.
+            Se a despesa já existe no planejamento do mês, prefira registrar a baixa pela central de movimentações.
+        </p>
+    </div>
+
@@ -37 +69 @@
-                                   th:field="*{description}" maxlength="255" placeholder="Ex.: aluguel da sala" required>
+                                   th:field="*{description}" maxlength="255" placeholder="Ex: aluguel da sala, internet da empresa" required>
@@ -45,2 +77,2 @@
-                                       th:field="*{amountPaid}" inputmode="decimal" placeholder="0,00" required>
-                                <small class="text-muted">Aceita 19,90, 19.90 e 1.234,56.</small>
+                                       th:field="*{amountPaid}" inputmode="decimal" placeholder="Ex: 980,00" required>
+                                <small class="pf-form-help">Aceita 19,90, 19.90 e 1.234,56.</small>
@@ -56,0 +89 @@
+                                <small class="pf-form-help">Mês ao qual esse gasto deve pertencer nos relatórios.</small>
@@ -60 +93,7 @@
-                                <label class="form-label" for="status">Status</label>
+                                <label class="form-label" for="status">
+                                    Status
+                                    <span class="pf-tooltip-icon"
+                                          title="Pago confirma a saída. Cancelado preserva o histórico sem considerar o valor no fechamento.">
+                                        <i class="bi bi-question-circle"></i>
+                                    </span>
+                                </label>
@@ -110 +149 @@
-                                       placeholder="Opcional">
+                                       placeholder="Opcional — ex: Condominio do predio">
@@ -123 +162 @@
-                                      placeholder="Detalhes adicionais do pagamento"></textarea>
+                                      placeholder="Opcional — ex: Pago com desconto de 5%"></textarea>
diff --git a/src/main/resources/templates/financeiro-pessoal/despesas/pagamentos/list.html b/src/main/resources/templates/financeiro-pessoal/despesas/pagamentos/list.html
index 1c7beac..c7f0176 100644
--- a/src/main/resources/templates/financeiro-pessoal/despesas/pagamentos/list.html
+++ b/src/main/resources/templates/financeiro-pessoal/despesas/pagamentos/list.html
@@ -1,0 +2,8 @@
+<!--
+  TELA: Lista de Pagamentos Avulsos
+  ROTA: /financeiro-pessoal/despesas/pagamentos?mes=YYYY-MM
+  CONTROLLER: ExpensePaymentController
+
+  Lista pagamentos de despesas com filtros por período, conta,
+  forma de pagamento e status.
+-->
@@ -8,0 +17,6 @@
+    <nav class="pf-breadcrumb" aria-label="Breadcrumb">
+        <a class="pf-breadcrumb__link" th:href="@{/financeiro-pessoal(mes=${selectedMonthValue})}">Painel</a>
+        <span class="pf-breadcrumb__separator">›</span>
+        <span class="pf-breadcrumb__current">Pagamentos avulsos</span>
+    </nav>
+
@@ -19,0 +34,16 @@
+    <div class="pf-tip-card" data-pf-tip-card>
+        <div class="pf-tip-card__header">
+            <span class="pf-tip-card__title">
+                <i class="bi bi-lightbulb"></i>
+                <span>Filtro rápido</span>
+            </span>
+            <button type="button" class="pf-tip-card__close" data-pf-tip-close aria-label="Fechar dica">
+                <i class="bi bi-x-lg"></i>
+            </button>
+        </div>
+        <p>
+            Esta tela ajuda quando você quer acompanhar pagamentos soltos, sem depender de um item planejado.
+            Se estiver procurando uma conta do planejamento do mês, use a central de movimentações.
+        </p>
+    </div>
+
@@ -28 +58 @@
-                           placeholder="Ex.: aluguel, internet"
+                           placeholder="Ex: aluguel, internet, limpeza"
@@ -225 +255,3 @@
-                        <td colspan="9" class="text-center text-muted py-4">Nenhum pagamento encontrado com os filtros atuais.</td>
+                        <td colspan="9" class="py-4">
+                            <th:block th:replace="~{fragments/redesign :: emptyState('bi-receipt-cutoff', 'Nenhum pagamento encontrado', 'Ajuste os filtros ou cadastre um novo pagamento avulso para começar a acompanhar essas saídas.', null, null)}"></th:block>
+                        </td>
diff --git a/src/main/resources/templates/financeiro-pessoal/lancamento-historico.html b/src/main/resources/templates/financeiro-pessoal/lancamento-historico.html
index ea10790..291d157 100644
--- a/src/main/resources/templates/financeiro-pessoal/lancamento-historico.html
+++ b/src/main/resources/templates/financeiro-pessoal/lancamento-historico.html
@@ -1,0 +2,6 @@
+<!--
+  TELA: Lancamento Histórico
+  ARQUIVO: src\main\resources\templatés\financeiro-pessoal\lancamento-histórico.html
+  TIPO: Template Thymeleaf
+  OBJETIVO: Documenta a função deste template dentro da interface web.
+-->
@@ -15 +21 @@
-                <span>Movimentos</span>
+                <span>Movimentações</span>
@@ -26 +32 @@
-        <th:block th:replace="~{fragments/app-ui :: metricCard('Movimentos previstos', ${#lists.size(plan.items())}, 'Itens ainda planejados ou já concluídos no período.', 'investment', 'bi-list-ul')}"></th:block>
+        <th:block th:replace="~{fragments/app-ui :: metricCard('Itens previstos', ${#lists.size(plan.items())}, 'Itens ainda planejados ou já concluídos no período.', 'investment', 'bi-list-ul')}"></th:block>
diff --git a/src/main/resources/templates/financeiro-pessoal/lancamento-novo-item.html b/src/main/resources/templates/financeiro-pessoal/lancamento-novo-item.html
index bec8390..a346dce 100644
--- a/src/main/resources/templates/financeiro-pessoal/lancamento-novo-item.html
+++ b/src/main/resources/templates/financeiro-pessoal/lancamento-novo-item.html
@@ -1,0 +2,6 @@
+<!--
+  TELA: Lancamento Novo Item
+  ARQUIVO: src\main\resources\templatés\financeiro-pessoal\lancamento-novo-item.html
+  TIPO: Template Thymeleaf
+  OBJETIVO: Reune os formularios de criacao de lancamentos unicos, parcelados e recorrentes.
+-->
@@ -283 +289,25 @@
-    <th:block th:replace="~{fragments/page-header :: pageHeaderNoActions('Novo Lançamento', |Cadastro para ${selectedMonthLabel}|)}"></th:block>
+    <nav class="pf-breadcrumb" aria-label="Breadcrumb">
+        <a class="pf-breadcrumb__link" th:href="@{/financeiro-pessoal(mes=${selectedMonthValue})}">Painel</a>
+        <span class="pf-breadcrumb__separator">›</span>
+        <a class="pf-breadcrumb__link" th:href="@{/financeiro-pessoal/movimentos(mes=${selectedMonthValue})}">Movimentações</a>
+        <span class="pf-breadcrumb__separator">›</span>
+        <span class="pf-breadcrumb__current">Novo lançamento</span>
+    </nav>
+
+    <th:block th:replace="~{fragments/page-header :: pageHeaderNoActions('Novo lançamento', |Cadastro para ${selectedMonthLabel}|)}"></th:block>
+
+    <div class="pf-tip-card" data-pf-tip-card>
+        <div class="pf-tip-card__header">
+            <span class="pf-tip-card__title">
+                <i class="bi bi-lightbulb"></i>
+                <span>Escolha o tipo certo</span>
+            </span>
+            <button type="button" class="pf-tip-card__close" data-pf-tip-close aria-label="Fechar dica">
+                <i class="bi bi-x-lg"></i>
+            </button>
+        </div>
+        <p>
+            Use <strong>Único</strong> para algo que acontece uma vez. Use <strong>Parcelado</strong> para compras divididas.
+            Use <strong>Recorrente</strong> para contas que se repetem ao longo dos meses.
+        </p>
+    </div>
@@ -351 +381 @@
-                                    <input type="text" class="form-control" th:field="*{description}" maxlength="255" required>
+                                    <input type="text" class="form-control" th:field="*{description}" maxlength="255" placeholder="Ex: Aluguel, Conta de luz, Netflix" required>
@@ -355 +385,7 @@
-                                    <label class="form-label">Tipo de vencimento</label>
+                                    <label class="form-label">
+                                        Tipo de vencimento
+                                        <span class="pf-tooltip-icon"
+                                              title="Dia do mês repete um dia fixo. Fatura usa o ciclo do cartão. Variável deixa sem data fixa.">
+                                            <i class="bi bi-question-circle"></i>
+                                        </span>
+                                    </label>
@@ -364 +400 @@
-                                    <input type="number" class="form-control" th:field="*{dueDay}" min="1" max="31" data-pf-plan-due-day>
+                                    <input type="number" class="form-control" th:field="*{dueDay}" min="1" max="31" placeholder="Ex: 10" data-pf-plan-due-day>
@@ -383 +419,2 @@
-                                    <input type="number" class="form-control" th:field="*{amountExpected}" min="0.00" step="0.01" required>
+                                    <input type="number" class="form-control" th:field="*{amountExpected}" min="0.00" step="0.01" placeholder="Ex: 1500,00" required>
+                                    <small class="pf-form-help">Informe o valor que você espera pagar ou receber neste item.</small>
diff --git a/src/main/resources/templates/financeiro-pessoal/lancamento-totais.html b/src/main/resources/templates/financeiro-pessoal/lancamento-totais.html
index eff5b1b..c9f8660 100644
--- a/src/main/resources/templates/financeiro-pessoal/lancamento-totais.html
+++ b/src/main/resources/templates/financeiro-pessoal/lancamento-totais.html
@@ -1,0 +2,6 @@
+<!--
+  TELA: Lancamento Totais
+  ARQUIVO: src\main\resources\templatés\financeiro-pessoal\lancamento-totais.html
+  TIPO: Template Thymeleaf
+  OBJETIVO: Documenta a função deste template dentro da interface web.
+-->
@@ -15 +21 @@
-                <span>Voltar aos movimentos</span>
+                <span>Voltar às movimentações</span>
diff --git a/src/main/resources/templates/financeiro-pessoal/movimento-form.html b/src/main/resources/templates/financeiro-pessoal/movimento-form.html
index 65fd4fa..56713ce 100644
--- a/src/main/resources/templates/financeiro-pessoal/movimento-form.html
+++ b/src/main/resources/templates/financeiro-pessoal/movimento-form.html
@@ -1,0 +2,6 @@
+<!--
+  TELA: Movimento Form
+  ARQUIVO: src\main\resources\templatés\financeiro-pessoal\movimento-form.html
+  TIPO: Template Thymeleaf
+  OBJETIVO: Registra a baixa ou o recebimento real de um item planejádo.
+-->
@@ -5 +11 @@
-    <title>Registrar movimento</title>
+    <title>Registrar movimentação</title>
@@ -8,0 +15,8 @@
+    <nav class="pf-breadcrumb" aria-label="Breadcrumb">
+        <a class="pf-breadcrumb__link" th:href="@{/financeiro-pessoal(mes=${selectedMonthValue})}">Painel</a>
+        <span class="pf-breadcrumb__separator">›</span>
+        <a class="pf-breadcrumb__link" th:href="@{/financeiro-pessoal/movimentos(mes=${selectedMonthValue})}">Movimentações</a>
+        <span class="pf-breadcrumb__separator">›</span>
+        <span class="pf-breadcrumb__current" th:text="${movementPageTitle}">Registrar movimentação</span>
+    </nav>
+
@@ -21,0 +36,16 @@
+    <div class="pf-tip-card" data-pf-tip-card>
+        <div class="pf-tip-card__header">
+            <span class="pf-tip-card__title">
+                <i class="bi bi-lightbulb"></i>
+                <span>Quando usar esta tela</span>
+            </span>
+            <button type="button" class="pf-tip-card__close" data-pf-tip-close aria-label="Fechar dica">
+                <i class="bi bi-x-lg"></i>
+            </button>
+        </div>
+        <p>
+            Registre aqui o que <strong>realmente aconteceu</strong>: um pagamento, um recebimento ou uma baixa parcial.
+            Se o valor pago for diferente do previsto, use o campo de diferença para explicar se houve desconto, juros ou parcial.
+        </p>
+    </div>
+
@@ -75 +105,7 @@
-                        <label class="form-label" for="accountId">Conta</label>
+                        <label class="form-label" for="accountId">
+                            Conta
+                            <span class="pf-tooltip-icon"
+                                  title="Escolha de onde saiu ou para onde entrou o dinheiro desta movimentação.">
+                                <i class="bi bi-question-circle"></i>
+                            </span>
+                        </label>
@@ -85 +121 @@
-                            Nenhuma conta ativa encontrada. Cadastre ao menos uma conta antes de registrar o movimento.
+                            Nenhuma conta ativa encontrada. Cadastre ao menos uma conta antes de registrar a movimentação.
@@ -101,2 +137,2 @@
-                        <input id="amountPaid" type="text" class="form-control" th:field="*{amountPaid}" placeholder="0,00" required>
-                        <small class="text-muted">Aceita 19,90, 19.90 e 1.234,56.</small>
+                        <input id="amountPaid" type="text" class="form-control" th:field="*{amountPaid}" placeholder="Ex: 1.250,00" required>
+                        <small class="pf-form-help">Aceita 19,90, 19.90 e 1.234,56.</small>
@@ -109 +145 @@
-                        <textarea id="note" class="form-control" rows="4" th:field="*{note}" maxlength="500"></textarea>
+                        <textarea id="note" class="form-control" rows="4" th:field="*{note}" maxlength="500" placeholder="Opcional — ex: Pago com desconto de 5%"></textarea>
diff --git a/src/main/resources/templates/financeiro-pessoal/movimento-historico.html b/src/main/resources/templates/financeiro-pessoal/movimento-historico.html
index 1bb33ca..cfb6ab2 100644
--- a/src/main/resources/templates/financeiro-pessoal/movimento-historico.html
+++ b/src/main/resources/templates/financeiro-pessoal/movimento-historico.html
@@ -1,0 +2,6 @@
+<!--
+  TELA: Movimento Histórico
+  ARQUIVO: src\main\resources\templatés\financeiro-pessoal\movimento-histórico.html
+  TIPO: Template Thymeleaf
+  OBJETIVO: Documenta a função deste template dentro da interface web.
+-->
@@ -5 +11 @@
-    <title>Histórico de movimentos</title>
+    <title>Histórico de movimentações</title>
@@ -9 +15 @@
-    <th:block th:replace="~{fragments/page-header :: pageHeader('Histórico de movimentos', |Movimentos do item ${summary.description()}|, ~{::headerActions})}">
+    <th:block th:replace="~{fragments/page-header :: pageHeader('Histórico de movimentações', |Movimentações do item ${summary.description()}|, ~{::headerActions})}">
@@ -13 +19 @@
-                Novo movimento
+                Nova movimentação
@@ -61 +67 @@
-            <span class="text-muted small" th:text="|Movimentos: ${#lists.size(movements)}|">0</span>
+            <span class="text-muted small" th:text="|Movimentações: ${#lists.size(movements)}|">0</span>
@@ -112 +118 @@
-                        <td colspan="7" class="text-center text-muted py-4">Nenhum movimento registrado para este item.</td>
+                        <td colspan="7" class="text-center text-muted py-4">Nenhuma movimentação registrada para este item.</td>
diff --git a/src/main/resources/templates/financeiro-pessoal/movimentos-tipo.html b/src/main/resources/templates/financeiro-pessoal/movimentos-tipo.html
index 11a888d..eb05ed6 100644
--- a/src/main/resources/templates/financeiro-pessoal/movimentos-tipo.html
+++ b/src/main/resources/templates/financeiro-pessoal/movimentos-tipo.html
@@ -1,0 +2,6 @@
+<!--
+  TELA: Movimentos Tipo
+  ARQUIVO: src\main\resources\templatés\financeiro-pessoal\movimentos-tipo.html
+  TIPO: Template Thymeleaf
+  OBJETIVO: Documenta a função deste template dentro da interface web.
+-->
@@ -6 +12 @@
-    <title th:text="${movementPageTitle}">Movimentos</title>
+    <title th:text="${movementPageTitle}">Movimentações</title>
diff --git a/src/main/resources/templates/financeiro-pessoal/movimentos.html b/src/main/resources/templates/financeiro-pessoal/movimentos.html
index dda79ae..ab771d2 100644
--- a/src/main/resources/templates/financeiro-pessoal/movimentos.html
+++ b/src/main/resources/templates/financeiro-pessoal/movimentos.html
@@ -1,0 +2,8 @@
+<!--
+  TELA: Central de Movimentos
+  ROTA: /financeiro-pessoal/movimentos?mes=YYYY-MM
+  CONTROLLER: PersonalFinanceController#movimentos()
+
+  Reune pendencias, pagamentos confirmados, filtros operacionais e acoes
+  do dia a dia para o fechamento do mes.
+-->
@@ -6 +14 @@
-    <title>Movimentos</title>
+    <title>Movimentações</title>
@@ -9,0 +18,6 @@
+    <nav class="pf-breadcrumb" aria-label="Breadcrumb">
+        <a class="pf-breadcrumb__link" th:href="@{/financeiro-pessoal(mes=${selectedMonthValue})}">Painel</a>
+        <span class="pf-breadcrumb__separator">›</span>
+        <span class="pf-breadcrumb__current">Movimentações</span>
+    </nav>
+
@@ -30,0 +45,16 @@
+    <div class="pf-tip-card" data-pf-tip-card>
+        <div class="pf-tip-card__header">
+            <span class="pf-tip-card__title">
+                <i class="bi bi-lightbulb"></i>
+                <span>Planejado x realizado</span>
+            </span>
+            <button type="button" class="pf-tip-card__close" data-pf-tip-close aria-label="Fechar dica">
+                <i class="bi bi-x-lg"></i>
+            </button>
+        </div>
+        <p>
+            Os <strong>itens em aberto</strong> mostram o que ainda falta receber, pagar ou revisar.
+            Os <strong>lançamentos confirmados</strong> mostram o que já aconteceu de verdade no mês.
+        </p>
+    </div>
+
@@ -85 +115 @@
-                           placeholder="Buscar descrição, conta ou observação"
+                           placeholder="Ex: aluguel, Nubank, farmácia"
@@ -108 +138,7 @@
-                        <label class="form-label" for="filterTipo">Tipo</label>
+                        <label class="form-label" for="filterTipo">
+                            Tipo
+                            <span class="pf-tooltip-icon"
+                                  title="Use para ver só receitas, despesas ou investimentos.">
+                                <i class="bi bi-question-circle"></i>
+                            </span>
+                        </label>
@@ -117 +153,7 @@
-                        <label class="form-label" for="filterStatus">Status</label>
+                        <label class="form-label" for="filterStatus">
+                            Status
+                            <span class="pf-tooltip-icon"
+                                  title="Pendentes incluem o que ainda falta baixar. Parciais indicam baixa incompleta.">
+                                <i class="bi bi-question-circle"></i>
+                            </span>
+                        </label>
@@ -151 +193 @@
-                               placeholder="Descrição, conta, observação"
+                               placeholder="Ex: energia, carteira, recebido"
@@ -152,0 +195 @@
+                        <small class="pf-form-help">A busca aceita descrição, conta, observação e valores digitados como texto.</small>
@@ -255 +298 @@
-                                              onsubmit="return confirm('Confirma a inativação deste item? O histórico será preservado.');">
+                                              onsubmit="return confirm('Inativar este item? Ele sai das telas operacionais, mas o histórico já registrado continua preservado.');">
@@ -267 +310 @@
-                                              onsubmit="return confirm('Confirma a exclusão definitiva deste lançamento?');">
+                                              onsubmit="return confirm('Excluir este item de forma definitiva? Use essa opção apenas quando o lançamento foi criado por engano.');">
@@ -351 +394 @@
-                                              onsubmit="return confirm('Cancelar esta baixa? O histórico será preservado.');">
+                                              onsubmit="return confirm('Cancelar esta baixa? O histórico continua salvo para auditoria e o item volta a ficar pendente.');">
diff --git a/src/main/resources/templates/financeiro-pessoal/painel.html b/src/main/resources/templates/financeiro-pessoal/painel.html
index 0991958..80899c0 100644
--- a/src/main/resources/templates/financeiro-pessoal/painel.html
+++ b/src/main/resources/templates/financeiro-pessoal/painel.html
@@ -1,0 +2,8 @@
+<!--
+  TELA: Painel Financeiro
+  ROTA: /financeiro-pessoal?mes=YYYY-MM
+  CONTROLLER: PersonalFinanceController#painel()
+
+  Consolida a leitura do mes em um unico lugar: saldo, receitas, despesas,
+  ultimos lancamentos, contas e próximos vencimentos.
+-->
@@ -13,0 +22,6 @@
+    <nav class="pf-breadcrumb" aria-label="Breadcrumb">
+        <a class="pf-breadcrumb__link" th:href="@{/financeiro-pessoal(mes=${selectedMonthValue})}">Painel</a>
+        <span class="pf-breadcrumb__separator">›</span>
+        <span class="pf-breadcrumb__current">Painel do mês</span>
+    </nav>
+
@@ -55 +69 @@
-                    <span>Ver movimentos</span>
+                    <span>Ver movimentações</span>
@@ -66,0 +81,17 @@
+    <div class="pf-tip-card" data-pf-tip-card>
+        <div class="pf-tip-card__header">
+            <span class="pf-tip-card__title">
+                <i class="bi bi-lightbulb"></i>
+                <span>Como ler este painel</span>
+            </span>
+            <button type="button" class="pf-tip-card__close" data-pf-tip-close aria-label="Fechar dica">
+                <i class="bi bi-x-lg"></i>
+            </button>
+        </div>
+        <p>
+            O <strong>planejado</strong> mostra o que você esperava para o mês. O <strong>realizado</strong>
+            mostra o que já entrou ou saiu de fato. Use os cards abaixo para identificar rápido se ainda há
+            valores a receber, contas a pagar ou risco de fechar o mês no vermelho.
+        </p>
+    </div>
+
@@ -109 +140,7 @@
-                        <span class="app-summary-card__label">Receitas</span>
+                        <span class="app-summary-card__label">
+                            Receitas
+                            <span class="pf-tooltip-icon"
+                                  title="Tudo o que entrou de fato no mês. O texto 'A receber' mostra o que ainda falta confirmar.">
+                                <i class="bi bi-question-circle"></i>
+                            </span>
+                        </span>
@@ -131 +168,7 @@
-                        <span class="app-summary-card__label">Despesas</span>
+                        <span class="app-summary-card__label">
+                            Despesas
+                            <span class="pf-tooltip-icon"
+                                  title="Tudo o que já saiu no mês. O texto 'A pagar' mostra o que ainda está pendente.">
+                                <i class="bi bi-question-circle"></i>
+                            </span>
+                        </span>
@@ -155 +198,7 @@
-                        <span class="app-summary-card__label">Saldo</span>
+                        <span class="app-summary-card__label">
+                            Saldo
+                            <span class="pf-tooltip-icon"
+                                  title="Saldo disponível hoje. O valor projetado considera o que ainda falta receber e pagar.">
+                                <i class="bi bi-question-circle"></i>
+                            </span>
+                        </span>
@@ -311 +360 @@
-                          th:text="|${#lists.size(dashboard.upcomingBills())} itens|">
+                          th:text="|${#lists.size(dashboard.upcomingBills())} ${#lists.size(dashboard.upcomingBills()) == 1 ? 'item' : 'itens'}|">
diff --git a/src/main/resources/templates/financeiro-pessoal/relatorios-categorias.html b/src/main/resources/templates/financeiro-pessoal/relatorios-categorias.html
index 48d945d..a057429 100644
--- a/src/main/resources/templates/financeiro-pessoal/relatorios-categorias.html
+++ b/src/main/resources/templates/financeiro-pessoal/relatorios-categorias.html
@@ -1,0 +2,6 @@
+<!--
+  TELA: Relatórios Catégorias
+  ARQUIVO: src\main\resources\templatés\financeiro-pessoal\relatórios-catégorias.html
+  TIPO: Template Thymeleaf
+  OBJETIVO: Mostra a performance das classificações no período filtrado.
+-->
@@ -8,0 +15,8 @@
+    <nav class="pf-breadcrumb" aria-label="Breadcrumb">
+        <a class="pf-breadcrumb__link" th:href="@{/financeiro-pessoal(mes=${selectedMonthValue})}">Painel</a>
+        <span class="pf-breadcrumb__separator">›</span>
+        <a class="pf-breadcrumb__link" th:href="@{/financeiro-pessoal/relatorios/categorias(mes=${selectedMonthValue})}">Relatórios</a>
+        <span class="pf-breadcrumb__separator">›</span>
+        <span class="pf-breadcrumb__current">Classificações</span>
+    </nav>
+
@@ -14,0 +29,16 @@
+    <div class="pf-tip-card" data-pf-tip-card>
+        <div class="pf-tip-card__header">
+            <span class="pf-tip-card__title">
+                <i class="bi bi-lightbulb"></i>
+                <span>Leitura por categoria</span>
+            </span>
+            <button type="button" class="pf-tip-card__close" data-pf-tip-close aria-label="Fechar dica">
+                <i class="bi bi-x-lg"></i>
+            </button>
+        </div>
+        <p>
+            Use este relatório para descobrir em quais categorias você mais gastou ou recebeu no período.
+            A coluna de diferença ajuda a ver rapidamente o que ficou acima ou abaixo do esperado.
+        </p>
+    </div>
+
diff --git a/src/main/resources/templates/financeiro-pessoal/relatorios-fluxo.html b/src/main/resources/templates/financeiro-pessoal/relatorios-fluxo.html
index 2e7203f..b85ccac 100644
--- a/src/main/resources/templates/financeiro-pessoal/relatorios-fluxo.html
+++ b/src/main/resources/templates/financeiro-pessoal/relatorios-fluxo.html
@@ -1,0 +2,6 @@
+<!--
+  TELA: Relatórios Fluxo
+  ARQUIVO: src\main\resources\templatés\financeiro-pessoal\relatórios-fluxo.html
+  TIPO: Template Thymeleaf
+  OBJETIVO: Apresenta o fluxo de caixa consolidado e a evolucao de entradas, saidas e saldo.
+-->
@@ -8,0 +15,8 @@
+    <nav class="pf-breadcrumb" aria-label="Breadcrumb">
+        <a class="pf-breadcrumb__link" th:href="@{/financeiro-pessoal(mes=${selectedMonthValue})}">Painel</a>
+        <span class="pf-breadcrumb__separator">›</span>
+        <a class="pf-breadcrumb__link" th:href="@{/financeiro-pessoal/relatorios/fluxo(mes=${selectedMonthValue})}">Relatórios</a>
+        <span class="pf-breadcrumb__separator">›</span>
+        <span class="pf-breadcrumb__current">Fluxo de caixa</span>
+    </nav>
+
@@ -14,0 +29,16 @@
+    <div class="pf-tip-card" data-pf-tip-card>
+        <div class="pf-tip-card__header">
+            <span class="pf-tip-card__title">
+                <i class="bi bi-lightbulb"></i>
+                <span>Como interpretar</span>
+            </span>
+            <button type="button" class="pf-tip-card__close" data-pf-tip-close aria-label="Fechar dica">
+                <i class="bi bi-x-lg"></i>
+            </button>
+        </div>
+        <p>
+            Este relatório junta <strong>entradas</strong>, <strong>saídas</strong> e <strong>saldo</strong> no mesmo recorte.
+            Se o saldo estiver caindo mês após mês, vale revisar as categorias com maior peso e os itens ainda pendentes.
+        </p>
+    </div>
+
diff --git a/src/main/resources/templates/financeiro-pessoal/relatorios-periodo.html b/src/main/resources/templates/financeiro-pessoal/relatorios-periodo.html
index f26533e..de65691 100644
--- a/src/main/resources/templates/financeiro-pessoal/relatorios-periodo.html
+++ b/src/main/resources/templates/financeiro-pessoal/relatorios-periodo.html
@@ -1,0 +2,6 @@
+<!--
+  TELA: Relatórios Período
+  ARQUIVO: src\main\resources\templatés\financeiro-pessoal\relatórios-período.html
+  TIPO: Template Thymeleaf
+  OBJETIVO: Compara resultados mensais e indicadores consolidados ao longo de um intervalo.
+-->
@@ -8,0 +15,8 @@
+    <nav class="pf-breadcrumb" aria-label="Breadcrumb">
+        <a class="pf-breadcrumb__link" th:href="@{/financeiro-pessoal(mes=${selectedMonthValue})}">Painel</a>
+        <span class="pf-breadcrumb__separator">›</span>
+        <a class="pf-breadcrumb__link" th:href="@{/financeiro-pessoal/relatorios/periodo(mes=${selectedMonthValue})}">Relatórios</a>
+        <span class="pf-breadcrumb__separator">›</span>
+        <span class="pf-breadcrumb__current">Comparativo por período</span>
+    </nav>
+
@@ -14,0 +29,16 @@
+    <div class="pf-tip-card" data-pf-tip-card>
+        <div class="pf-tip-card__header">
+            <span class="pf-tip-card__title">
+                <i class="bi bi-lightbulb"></i>
+                <span>Leitura de tendência</span>
+            </span>
+            <button type="button" class="pf-tip-card__close" data-pf-tip-close aria-label="Fechar dica">
+                <i class="bi bi-x-lg"></i>
+            </button>
+        </div>
+        <p>
+            Este comparativo ajuda a entender se seu resultado esta melhorando ou piorando ao longo dos meses.
+            Olhe principalmente para o saldo e para os grupos de despesa com maior peso no recorte.
+        </p>
+    </div>
+
diff --git a/src/main/resources/templates/financeiro-pessoal/transferencias/detail.html b/src/main/resources/templates/financeiro-pessoal/transferencias/detail.html
index e4d6167..b7eda5f 100644
--- a/src/main/resources/templates/financeiro-pessoal/transferencias/detail.html
+++ b/src/main/resources/templates/financeiro-pessoal/transferencias/detail.html
@@ -1,0 +2,6 @@
+<!--
+  TELA: Detail
+  ARQUIVO: src\main\resources\templatés\financeiro-pessoal\transferencias\detail.html
+  TIPO: Template Thymeleaf
+  OBJETIVO: Documenta a função deste template dentro da interface web.
+-->
@@ -87 +93 @@
-                <div class="card-header"><strong>Movimentos gerados</strong></div>
+                <div class="card-header"><strong>Movimentações geradas</strong></div>
@@ -113 +119 @@
-                                <td colspan="5" class="text-center text-muted py-4">Nenhum movimento vinculado encontrado.</td>
+                                <td colspan="5" class="text-center text-muted py-4">Nenhuma movimentação vinculada encontrada.</td>
diff --git a/src/main/resources/templates/financeiro-pessoal/transferencias/form.html b/src/main/resources/templates/financeiro-pessoal/transferencias/form.html
index 2f3f7c4..acee579 100644
--- a/src/main/resources/templates/financeiro-pessoal/transferencias/form.html
+++ b/src/main/resources/templates/financeiro-pessoal/transferencias/form.html
@@ -1,0 +2,6 @@
+<!--
+  TELA: Form
+  ARQUIVO: src\main\resources\templatés\financeiro-pessoal\transferencias\form.html
+  TIPO: Template Thymeleaf
+  OBJETIVO: Documenta a função deste template dentro da interface web.
+-->
diff --git a/src/main/resources/templates/fragments/navbar.html b/src/main/resources/templates/fragments/navbar.html
index aa65e25..86e7906 100644
--- a/src/main/resources/templates/fragments/navbar.html
+++ b/src/main/resources/templates/fragments/navbar.html
@@ -1,0 +2,6 @@
+<!--
+  TELA: Navbar
+  ARQUIVO: src\main\resources\templatés\fragments\navbar.html
+  TIPO: Fragmento Thymeleaf
+  OBJETIVO: Documenta a função deste template dentro da interface web.
+-->
@@ -75 +81 @@
-                                <span class="fd-sidebar-link__title">Movimentos</span>
+                                <span class="fd-sidebar-link__title">Movimentações</span>
@@ -119 +125 @@
-                    <span class="fd-sidebar__section-label">Movimentos</span>
+                    <span class="fd-sidebar__section-label">Movimentações</span>
diff --git a/src/main/resources/templates/metas/index.html b/src/main/resources/templates/metas/index.html
index 4a0b5a4..0126254 100644
--- a/src/main/resources/templates/metas/index.html
+++ b/src/main/resources/templates/metas/index.html
@@ -1,0 +2,6 @@
+<!--
+  TELA: Index
+  ARQUIVO: src\main\resources\templatés\metas\index.html
+  TIPO: Template Thymeleaf
+  OBJETIVO: Documenta a função deste template dentro da interface web.
+-->
@@ -27 +33 @@
-            <th:block th:replace="~{fragments/app-ui :: metricCard('Quero juntar', |R$ ${#numbers.formatDecimal(goalBoard.totalSavingsTarget(),1,'POINT',2,'COMMA')}|, 'Valor total que voce quer guardar para seus objetivos.', 'income', 'bi-piggy-bank')}"></th:block>
+            <th:block th:replace="~{fragments/app-ui :: metricCard('Quero juntar', |R$ ${#numbers.formatDecimal(goalBoard.totalSavingsTarget(),1,'POINT',2,'COMMA')}|, 'Valor total que você quer guardar para seus objetivos.', 'income', 'bi-piggy-bank')}"></th:block>
@@ -30 +36 @@
-            <th:block th:replace="~{fragments/app-ui :: metricCard('Projecao de economia', |R$ ${#numbers.formatDecimal(goalBoard.totalProjectedSavings(),1,'POINT',2,'COMMA')}|, ${goalBoard.savingsProjectionDisclaimer()}, 'investment', 'bi-graph-up-arrow')}"></th:block>
+            <th:block th:replace="~{fragments/app-ui :: metricCard('Projeção de economia', |R$ ${#numbers.formatDecimal(goalBoard.totalProjectedSavings(),1,'POINT',2,'COMMA')}|, ${goalBoard.savingsProjectionDisclaimer()}, 'investment', 'bi-graph-up-arrow')}"></th:block>
@@ -33 +39 @@
-            <th:block th:replace="~{fragments/app-ui :: metricCard('Falta juntar', |R$ ${#numbers.formatDecimal(goalBoard.remainingSavingsGap(),1,'POINT',2,'COMMA')}|, 'Diferenca entre o objetivo total e a projecao atual.', 'expense', 'bi-hourglass-split')}"></th:block>
+            <th:block th:replace="~{fragments/app-ui :: metricCard('Falta juntar', |R$ ${#numbers.formatDecimal(goalBoard.remainingSavingsGap(),1,'POINT',2,'COMMA')}|, 'Diferença entre o objetivo total e a projeção atual.', 'expense', 'bi-hourglass-split')}"></th:block>
@@ -37 +43 @@
-    <th:block th:replace="~{fragments/data-table :: table('Projetos e sonhos', 'Defina quanto guardar, ate quando e acompanhe o ritmo da reserva.', null, ~{::goalTable})}">
+    <th:block th:replace="~{fragments/data-table :: table('Projetos e sonhos', 'Defina quanto guardar, até quando e acompanhe o ritmo da reserva.', null, ~{::goalTable})}">
@@ -134 +140 @@
-                            <label class="form-label">Quanto voce quer juntar</label>
+                            <label class="form-label">Quanto você quer juntar</label>
@@ -140 +146 @@
-                                <label class="form-label">Comecar a guardar em</label>
+                                <label class="form-label">Começar a guardar em</label>
diff --git a/src/main/resources/templates/planejamento/index.html b/src/main/resources/templates/planejamento/index.html
index 59166ab..10ba657 100644
--- a/src/main/resources/templates/planejamento/index.html
+++ b/src/main/resources/templates/planejamento/index.html
@@ -1,0 +2,8 @@
+<!--
+  TELA: Planejámento Financeiro
+  ROTA: /financeiro-pessoal/planejámento?mes=YYYY-MM
+  CONTROLLER: PersonalFinanceController#planejámentoFinanceiro()
+
+  Acompanha limites por catégoria para comparar o que foi planejádo
+  com o que já esta sendo consumido no mes.
+-->
@@ -8,0 +17,6 @@
+    <nav class="pf-breadcrumb" aria-label="Breadcrumb">
+        <a class="pf-breadcrumb__link" th:href="@{/financeiro-pessoal(mes=${selectedMonthValue})}">Painel</a>
+        <span class="pf-breadcrumb__separator">›</span>
+        <span class="pf-breadcrumb__current">Planejamento</span>
+    </nav>
+
@@ -19,0 +34,16 @@
+    <div class="pf-tip-card" data-pf-tip-card>
+        <div class="pf-tip-card__header">
+            <span class="pf-tip-card__title">
+                <i class="bi bi-lightbulb"></i>
+                <span>Dica de uso</span>
+            </span>
+            <button type="button" class="pf-tip-card__close" data-pf-tip-close aria-label="Fechar dica">
+                <i class="bi bi-x-lg"></i>
+            </button>
+        </div>
+        <p>
+            Comece pelas categorias que mais pesam no seu bolso. Assim você enxerga rápido quando o mês
+            está fugindo do combinado e consegue agir antes do fechamento.
+        </p>
+    </div>
+
@@ -73,0 +104,5 @@
+                                <tr th:if="${#lists.isEmpty(goalBoard.budgets())}">
+                                    <td colspan="6" class="py-4">
+                                        <th:block th:replace="~{fragments/redesign :: emptyState('bi-bullseye', 'Nenhum planejamento criado ainda', 'Comece definindo o limite de uma categoria importante do seu mês para acompanhar o consumo em tempo real.', null, null)}"></th:block>
+                                    </td>
+                                </tr>
@@ -117 +152,2 @@
-                            <input type="text" class="form-control" th:field="*{name}" maxlength="120" placeholder="Ex.: Planejamento alimentação" required>
+                            <input type="text" class="form-control" th:field="*{name}" maxlength="120" placeholder="Ex: Planejamento alimentação" required>
+                            <small class="pf-form-help">Use um nome que ajude você a entender de imediato qual limite está acompanhando.</small>
@@ -120 +156,7 @@
-                            <label class="form-label">Classificação</label>
+                            <label class="form-label">
+                                Classificação
+                                <span class="pf-tooltip-icon"
+                                      title="Escolha a categoria que vai receber o limite mensal.">
+                                    <i class="bi bi-question-circle"></i>
+                                </span>
+                            </label>
@@ -128,3 +170,9 @@
-                            <label class="form-label">Limite mensal</label>
-                            <input type="number" class="form-control" th:field="*{targetAmount}" min="0.01" step="0.01" required>
-                            <small class="text-muted">Defina o teto de gasto para essa classificação no ciclo.</small>
+                            <label class="form-label">
+                                Limite mensal
+                                <span class="pf-tooltip-icon"
+                                      title="Esse valor funciona como teto de gasto para a categoria neste mês.">
+                                    <i class="bi bi-question-circle"></i>
+                                </span>
+                            </label>
+                            <input type="number" class="form-control" th:field="*{targetAmount}" min="0.01" step="0.01" placeholder="Ex: 850,00" required>
+                            <small class="pf-form-help">Defina um teto realista. O sistema compara esse valor com o realizado para mostrar quanto ainda resta.</small>
@@ -135,0 +184 @@
+                                <small class="pf-form-help">Mês em que este planejamento passa a valer.</small>
@@ -139,0 +189 @@
+                                <small class="pf-form-help">Opcional. Deixe em branco para manter o limite ativo até você decidir encerrar.</small>
```
