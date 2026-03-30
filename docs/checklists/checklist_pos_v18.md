# Checklist Manual Pos-Implementacao (V18)

Data base de referencia: 17/02/2026
Escopo: empresa, cliente, contatos, ocupacoes, dashboard, contratos e responsividade.

## 1. Preparacao
1. Rodar migracoes em base limpa.
2. Validar login com usuario ADMIN e usuario operacional.
3. Confirmar menu `Cadastro` e `Movimento` carregando sem erros.

## 2. Empresa (Locadora)
1. Acessar `Cadastro > Empresas > Nova Empresa`.
2. Informar CNPJ valido e clicar em `Buscar CNPJ`.
3. Confirmar preenchimento automatico de:
   - razao social
   - nome fantasia
   - telefone
   - email
   - endereco completo (logradouro, numero, bairro, cidade, uf, pais)
4. Salvar e reabrir o cadastro para confirmar persistencia.
5. Testar CNPJ invalido e validar mensagem de erro.

## 3. Cliente (3 enderecos obrigatorios)
1. Acessar `Cadastro > Clientes > Novo Cliente`.
2. Preencher dados basicos e tentar salvar sem endereco de cobranca.
3. Validar bloqueio com mensagem de obrigatoriedade.
4. Preencher os 3 blocos obrigatorios:
   - Endereco do Cliente
   - Endereco de Cobranca
   - Endereco de Entrega/Obra
5. Salvar e editar novamente para confirmar consistencia.

## 4. Responsavel de Contratacao
1. No cliente, preencher apenas nome do responsavel e tentar salvar sem CPF.
2. Validar bloqueio por CPF obrigatorio quando bloco estiver preenchido.
3. Selecionar ocupacao cadastrada e salvar com CPF valido.
4. Reabrir cliente e confirmar dados persistidos.

## 5. CRUD Ocupacoes
1. Acessar `Cadastro > Ocupacoes do Responsavel`.
2. Cadastrar nova ocupacao.
3. Editar descricao.
4. Inativar ocupacao.
5. Confirmar que ocupacao inativa nao aparece no combo de cliente novo.

## 6. CRUD Telefones e E-mails de Cliente
1. Na lista de clientes, abrir `Telefones`.
2. Cadastrar multiplos telefones.
3. Marcar um como principal e confirmar que os demais foram desmarcados.
4. Tentar cadastrar telefone duplicado e validar bloqueio.
5. Repetir o mesmo fluxo para `E-mails`.

## 7. Enderecos (menu de manutencao)
1. Abrir `Cadastro > Enderecos (manutencao)`.
2. Confirmar tipos permitidos: `CLIENTE`, `COBRANCA`, `OBRA`.
3. Editar endereco existente e salvar.

## 8. Equipamentos (codigo somente visual)
1. Abrir `Cadastro > Equipamentos > Novo Grupo`.
2. Confirmar campo `Codigo do equipamento` em somente leitura.
3. Salvar e confirmar que o backend gerou o codigo final.

## 9. Dashboard
1. Abrir dashboard inicial e validar periodo padrao (dia 01 ate hoje).
2. Validar KPIs novos:
   - Contratos vencendo (7 dias)
   - Contratos vencidos
   - Faturas vencidas
3. Confirmar tabelas com no maximo 10 registros cada.
4. Clicar `mais informacoes` em cada card e validar rota destino.
5. Alterar periodo e confirmar recarga dos dados.
6. Testar exportacao PDF e Excel.
7. Abrir em 2 navegadores e validar atualizacao automatica (SSE).

## 10. Contratos (dados completos)
1. Abrir um contrato existente em `Contratos > Consultar`.
2. Validar bloco completo da locadora (dados cadastrais e endereco).
3. Validar bloco completo do locatario (dados, contatos, enderecos).
4. Gerar/reimprimir PDF de contrato e conferir os mesmos dados.
5. Gerar comprovantes (entrega, pedido devolucao, devolucao) e confirmar dados + campos de assinatura.

## 11. Responsividade (mobile)
1. Testar em viewport 390x844 (mobile).
2. Confirmar:
   - menu recolhido funciona
   - cards nao quebram layout
   - tabelas com rolagem horizontal quando necessario
   - botoes de acao continuam acessiveis
3. Testar formularios de empresa e cliente em mobile (scroll e foco).

## 12. Criterio de aceite final
1. Nenhum erro no console do navegador nas telas alteradas.
2. Nenhum erro HTTP 500 nos fluxos testados.
3. Build e testes automatizados passando (`mvn test`).
