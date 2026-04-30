# Roteiro Completo de Funcionalidades do Sistema

Documento de apoio para apresentacao, treinamento e repasse do escopo funcional do sistema Financeiro Pessoal.

## 1. Roteiro de apresentacao

### 1.1 Abertura

"Este sistema foi estruturado para concentrar o controle financeiro pessoal em um unico ambiente. Ele cobre o ciclo completo de planejamento, lancamento, baixa, acompanhamento e analise dos resultados por mes e por periodo."

"O acesso e protegido por login e cada usuario opera dentro do seu escopo. Os cadastros administrativos ficam restritos ao perfil administrador, enquanto os demais usuarios trabalham somente com os dados que podem acessar."

### 1.2 Acesso e seguranca

"Na entrada do sistema temos a autenticacao por usuario e senha. Tambem existe fluxo de esqueci minha senha, redefinicao por token, alteracao da propria senha e sugestao de senha forte."

"Depois do login, o usuario pode trocar o tema visual do sistema e seguir navegando sempre a partir do mes de referencia selecionado."

### 1.3 Painel principal

"Ao entrar no sistema, o usuario chega no painel. Esse painel entrega uma visao executiva do mes selecionado, com saldo anterior, saldo atual, entradas, despesas, contas pendentes, contas pagas e cobertura das despesas sobre a receita."

"No mesmo painel tambem mostramos a evolucao dos ultimos meses, os principais grupos de entrada, os principais grupos de despesa, as contas com movimentacao no periodo e as pendencias prioritarias. A ideia aqui e abrir o sistema e entender rapidamente como esta a saude financeira do mes."

### 1.4 Cadastros base

"Na area de cadastros, o administrador consegue manter os dados estruturais do sistema. Isso inclui usuarios, grupos de categoria, tipos de categoria, contas financeiras e cartoes."

"Nos usuarios, fazemos cadastro, atualizacao, inativacao e reativacao. Nos grupos e tipos de categoria, organizamos a classificacao entre receita, despesa e investimento. Nas contas, definimos por exemplo conta corrente, dinheiro e investimento, sempre vinculando a responsabilidade a um usuario. Nos cartoes, controlamos nome, dia de fechamento, dia de vencimento e usuario responsavel."

### 1.5 Planejamento mensal

"A area de lancamentos e o coracao do planejamento mensal. Nela conseguimos cadastrar os itens previstos do mes, com descricao, grupo, categoria, usuario responsavel, tipo de vencimento e valor esperado."

"O sistema suporta tres formatos de vencimento: dia fixo do mes, fatura de cartao e vencimento variavel. Alem disso, o planejamento cobre receitas, despesas e investimentos."

"Tambem temos o recurso de lancamento parcelado, que gera automaticamente todas as parcelas futuras, e o lancamento recorrente, que projeta os proximos meses para despesas ou receitas repetitivas. Isso reduz digitacao manual e padroniza o planejamento."

"Dentro desse modulo existe ainda a tabela de lancamentos, o resumo do mes, os totais por grupo e o historico do que ja foi realizado."

### 1.6 Baixas e movimentacoes

"Depois do planejamento, entramos na operacao do mes. A central de movimentos concentra o que esta pendente, o que foi pago, o que foi recebido e tudo o que ja teve movimentacao financeira."

"Nessa central, o usuario pode filtrar por mes, tipo de item, status, conta, usuario e busca textual. O sistema separa receitas de um lado e despesas e investimentos do outro, e ainda mostra os totais filtrados de previsto, realizado e pendente."

"Quando uma conta precisa ser baixada, podemos registrar o valor realizado de forma completa ou parcial, informando conta, data, observacao e o usuario que executou a baixa. Esse mesmo fluxo vale para recebimentos. Tambem existe a baixa rapida para agilizar o fechamento de itens simples."

"Se necessario, o movimento pode ser editado ou excluido, e cada item possui seu historico detalhado de lancamentos."

### 1.7 Contas a pagar e contas a receber

"Para facilitar a rotina, o sistema entrega atalhos especificos para contas a pagar, contas pagas, contas a receber e contas recebidas. Esses atalhos ja abrem a central de movimentos com o recorte correto, acelerando a operacao do dia a dia."

### 1.8 Pagamentos de despesas avulsas

"O sistema tambem possui um modulo de pagamentos de despesas avulsas. Nesse fluxo e possivel registrar um pagamento direto com descricao, valor pago, data, competencia, categoria de despesa, conta, forma de pagamento, observacao, favorecido e dados do comprovante."

"Esse pagamento pode ser consultado em detalhe, editado, cancelado e excluido. As formas de pagamento previstas incluem Pix, dinheiro, cartao de credito, cartao de debito, boleto, transferencia e outras."

### 1.9 Relatorios e exportacoes

"Na camada analitica, temos quatro visoes principais. O relatorio mensal resume um unico mes. O relatorio por periodo consolida uma faixa de meses. O relatorio de fluxo destaca entradas, saidas e saldo liquido ao longo do tempo. E o relatorio de categorias mostra a performance por categoria."

"Todos esses relatorios podem ser refinados por usuario, conta, grupo e status. Alem da consulta em tela, o sistema permite exportacao em PDF e Excel, e no relatorio por periodo tambem existe exportacao em CSV."

### 1.10 Encerramento

"Em resumo, o sistema cobre desde a configuracao inicial, passando pelo planejamento e pela execucao financeira, ate a visao gerencial e a exportacao dos dados. Isso cria um fluxo completo para controlar o mes, acompanhar pendencias e analisar o resultado com mais clareza."

## 2. Inventario completo de funcionalidades

### 2.1 Acesso e conta

- Login com autenticacao obrigatoria.
- Logout do sistema.
- Recuperacao de senha por e-mail.
- Redefinicao de senha com token.
- Alteracao da propria senha.
- Sugestao de senha forte.
- Troca de tema visual pelo usuario.
- Controle de permissao por perfil e por propriedade dos dados.

### 2.2 Painel

- Selecao do mes de analise.
- Exibicao de saldo anterior e saldo atual.
- Exibicao de entradas, despesas e fluxo do mes.
- Indicadores de contas pendentes e contas pagas.
- Percentual de cobertura das despesas sobre a receita.
- Comparativo previsto x realizado.
- Evolucao mensal em janela historica.
- Top 5 grupos de entrada.
- Top 5 grupos de despesa.
- Destaques de despesas.
- Contas com movimentacao no periodo.
- Lista de pendencias prioritarias.
- Atalhos rapidos de navegacao.

### 2.3 Cadastros administrativos

- Cadastro de usuarios.
- Atualizacao de usuarios.
- Inativacao de usuarios.
- Reativacao de usuarios.
- Cadastro de grupos de categoria.
- Atualizacao de grupos de categoria.
- Exclusao de grupos quando permitido pelas validacoes do sistema.
- Cadastro de tipos de categoria.
- Atualizacao de tipos de categoria.
- Inativacao de tipos de categoria.
- Reativacao de tipos de categoria.
- Exclusao de tipos de categoria.
- Cadastro de contas.
- Atualizacao de contas.
- Inativacao de contas.
- Reativacao de contas.
- Definicao do tipo da conta: conta corrente, dinheiro ou investimento.
- Vinculo da conta a um usuario responsavel.
- Cadastro de cartoes.
- Atualizacao de cartoes.
- Inativacao de cartoes.
- Reativacao de cartoes.
- Controle de dia de fechamento e dia de vencimento do cartao.

### 2.4 Planejamento de lancamentos

- Cadastro de item planejado por mes.
- Atualizacao de item planejado.
- Inativacao de item planejado.
- Classificacao por receita, despesa e investimento.
- Vinculo do item a grupo e tipo de categoria.
- Vinculo do item a usuario responsavel.
- Definicao do vencimento por dia do mes.
- Definicao do vencimento por fatura de cartao.
- Definicao do vencimento variavel.
- Registro do valor previsto.
- Resumo mensal do planejamento.
- Tabela completa de lancamentos do mes.
- Totais por grupo.
- Historico do realizado no mes.
- Criacao de modelo parcelado com geracao automatica das parcelas futuras.
- Criacao de modelo recorrente com projecao automatica para meses futuros.
- Inativacao de modelo recorrente ou parcelado.
- Controle de status do item: pendente, parcial e pago.

### 2.5 Movimentos e baixas

- Central unica de movimentos.
- Filtro por mes.
- Filtro por tipo do item.
- Filtro por status.
- Filtro por conta.
- Filtro por usuario.
- Busca textual por item, conta, observacao, data ou valor.
- Cards de resumo com totais filtrados.
- Lista de receitas do mes.
- Lista de despesas e investimentos do mes.
- Registro manual de pagamento ou recebimento.
- Registro parcial de valores.
- Registro total de valores.
- Informacao da conta utilizada na baixa.
- Informacao da data do movimento.
- Informacao do usuario que realizou a baixa.
- Campo de observacao no movimento.
- Edicao de movimento ja registrado.
- Exclusao de movimento.
- Baixa rapida de item planejado.
- Historico detalhado de movimentos por item.

### 2.6 Atalhos operacionais

- Visao direta de contas a pagar.
- Visao direta de contas pagas.
- Visao direta de contas a receber.
- Visao direta de contas recebidas.

### 2.7 Pagamentos de despesas avulsas

- Cadastro de pagamento avulso de despesa.
- Consulta detalhada do pagamento.
- Edicao do pagamento.
- Cancelamento do pagamento.
- Exclusao do pagamento.
- Informacao da competencia do pagamento.
- Escolha da categoria de despesa.
- Escolha da conta de pagamento.
- Registro da forma de pagamento.
- Registro de observacao.
- Registro do favorecido.
- Registro de nome do comprovante.
- Registro de URL do comprovante.
- Status do pagamento: pago ou cancelado.
- Formas de pagamento: Pix, dinheiro, cartao de credito, cartao de debito, boleto, transferencia e outro.

### 2.8 Relatorios

- Relatorio mensal.
- Relatorio por periodo.
- Relatorio de fluxo de caixa.
- Relatorio de performance por categorias.
- Filtro de relatorios por usuario.
- Filtro de relatorios por conta.
- Filtro de relatorios por grupo.
- Filtro de relatorios por status.
- Exportacao do relatorio mensal em PDF.
- Exportacao do relatorio mensal em Excel.
- Exportacao do relatorio por periodo em CSV.
- Exportacao do relatorio por periodo em PDF.
- Exportacao do relatorio por periodo em Excel.
- Exportacao do relatorio de fluxo em PDF.
- Exportacao do relatorio de fluxo em Excel.
- Exportacao do relatorio de categorias em PDF.
- Exportacao do relatorio de categorias em Excel.

## 3. Ordem sugerida para demonstracao ao vivo

1. Login e acesso inicial.
2. Painel com leitura dos KPIs do mes.
3. Cadastros base: usuarios, grupos, tipos, contas e cartoes.
4. Planejamento: criar um lancamento simples, um parcelado e um recorrente.
5. Movimentos: baixar um item pendente e mostrar o historico.
6. Atalhos de contas a pagar e contas a receber.
7. Pagamento avulso de despesa.
8. Relatorios com exportacao.

## 4. Observacao

Este roteiro foi montado com base nos controllers, services, templates e DTOs atualmente presentes no projeto.
