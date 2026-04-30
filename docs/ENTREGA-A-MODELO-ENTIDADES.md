# ENTREGA A — Modelo de Dados: Entidades e Relacionamentos

**Projeto:** Sistema de Fisioterapia com Financeiro Integrado  
**Arquitetura:** Single-tenant por banco | Multi-CNPJ por tenant

---

## 1. Visão Geral

Todas as tabelas operacionais possuem `codigo_cnpj` como chave lógica de isolamento. O usuário escolhe o CNPJ ativo ao logar; todas as operações são filtradas por esse valor.

| Grupo | Entidades |
|-------|-----------|
| **Núcleo** | empresa, usuario, codigo_contador |
| **Cadastros** | cliente, endereco |
| **Produto/Estoque** | item, deposito, estoque_quantitativo, equipamento_patrimonial |
| **Contratos** | contrato, contrato_periodo, contrato_periodo_item, contrato_evento |
| **Manutenção** | manutencao |
| **Vendas** | venda, venda_item |
| **Faturamento** | fatura, fatura_item, fatura_pagamento |
| **Financeiro** | conta_financeira, categoria_financeira, lancamento_financeiro |

---

## 2. Entidades e Colunas Principais

### 2.1 Núcleo

#### empresa
Identifica cada CNPJ dentro do tenant. `codigo_cnpj` é imutável e único.

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| id | BIGSERIAL | PK |
| codigo_cnpj | VARCHAR(14) NOT NULL UNIQUE | CNPJ sem formatação (imutável) |
| razao_social | VARCHAR(255) NOT NULL | |
| nome_fantasia | VARCHAR(255) | |
| inscricao_estadual | VARCHAR(20) | |
| ativo | BOOLEAN DEFAULT true | |
| created_at, created_by, updated_at, updated_by | Auditoria | |

---

#### usuario
Usuários do sistema para auditoria (created_by/updated_by). Pode ou não ser multi-CNPJ conforme o design de autenticação.

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| id | BIGSERIAL | PK |
| codigo_cnpj | VARCHAR(14) NOT NULL | CNPJ do tenant (ou NULL se super-admin) |
| email | VARCHAR(255) NOT NULL UNIQUE | |
| nome | VARCHAR(255) NOT NULL | |
| cpf | VARCHAR(11) | |
| ativo | BOOLEAN DEFAULT true | |
| created_at, created_by, updated_at, updated_by | Auditoria | |

---

#### codigo_contador
Controla sequenciais por entidade/ano/CNPJ para: codigo_cliente, codigo_endereco, codigo_equipamento, numero_contrato, numero_orcamento, numero_fatura, numero_manutencao, numero_venda.

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| id | BIGSERIAL | PK |
| codigo_cnpj | VARCHAR(14) NOT NULL | |
| entidade | VARCHAR(50) NOT NULL | cliente, endereco, equipamento, contrato, orcamento, fatura, manutencao, venda |
| ano | SMALLINT NOT NULL | Ano vigente |
| proximo_valor | BIGINT NOT NULL DEFAULT 1 | Próximo sequencial |
| created_at, updated_at | Auditoria | |

**UK:** (codigo_cnpj, entidade, ano)

---

### 2.2 Cadastros

#### cliente
Clientes da locadora. Nunca deletar; apenas INATIVAR quando houver histórico.

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| id | BIGSERIAL | PK |
| codigo_cnpj | VARCHAR(14) NOT NULL | |
| codigo_cliente | VARCHAR(20) NOT NULL | Gerado pelo codigo_contador |
| tipo | VARCHAR(20) NOT NULL | PESSOA_FISICA, PESSOA_JURIDICA |
| cpf_cnpj | VARCHAR(14) NOT NULL | CPF ou CNPJ |
| razao_social | VARCHAR(255) | PJ |
| nome | VARCHAR(255) | PF |
| email | VARCHAR(255) | |
| telefone | VARCHAR(20) | |
| inativo_em | TIMESTAMP | Preenchido quando inativado (nunca delete) |
| created_at, created_by, updated_at, updated_by | Auditoria | |

**UK:** (codigo_cnpj, codigo_cliente)

---

#### endereco
Endereços vinculados a clientes. `codigo_endereco` por CNPJ.

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| id | BIGSERIAL | PK |
| codigo_cnpj | VARCHAR(14) NOT NULL | |
| codigo_endereco | VARCHAR(20) NOT NULL | Gerado pelo codigo_contador |
| codigo_cliente | VARCHAR(20) NOT NULL | FK lógica → cliente |
| tipo | VARCHAR(20) | COMERCIAL, RESIDENCIAL, OBRA |
| cep | VARCHAR(8) | |
| logradouro | VARCHAR(255) | |
| numero | VARCHAR(20) | |
| complemento | VARCHAR(100) | |
| bairro | VARCHAR(100) | |
| cidade | VARCHAR(100) | |
| uf | CHAR(2) | |
| principal | BOOLEAN DEFAULT false | Endereço principal do cliente |
| created_at, created_by, updated_at, updated_by | Auditoria | |

**UK:** (codigo_cnpj, codigo_endereco)

---

### 2.3 Produto e Estoque

#### item
Produto unificado para locação e/ou venda.

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| id | BIGSERIAL | PK |
| codigo_cnpj | VARCHAR(14) NOT NULL | |
| codigo_item | VARCHAR(20) NOT NULL | Código interno único por CNPJ |
| descricao | VARCHAR(255) NOT NULL | |
| unidade | VARCHAR(10) NOT NULL | UN, M2, KG, HR, DIA, etc. |
| tipo_item | VARCHAR(20) NOT NULL | LOCACAO, VENDA, AMBOS |
| controle_patrimonial | BOOLEAN DEFAULT false | Se true, usa equipamento_patrimonial |
| valor_locacao_base | DECIMAL(15,2) | Valor diário base |
| valor_venda_base | DECIMAL(15,2) | Preço de venda |
| inativo_em | TIMESTAMP | Nunca delete |
| created_at, created_by, updated_at, updated_by | Auditoria | |

**UK:** (codigo_cnpj, codigo_item)

---

#### deposito
Depósitos (pools) onde o estoque é armazenado.

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| id | BIGSERIAL | PK |
| codigo_cnpj | VARCHAR(14) NOT NULL | |
| codigo_deposito | VARCHAR(20) NOT NULL | |
| descricao | VARCHAR(255) NOT NULL | |
| ativo | BOOLEAN DEFAULT true | |
| created_at, created_by, updated_at, updated_by | Auditoria | |

**UK:** (codigo_cnpj, codigo_deposito)

---

#### estoque_quantitativo
Estoque por item + depósito + CNPJ. Controle quantitativo.

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| id | BIGSERIAL | PK |
| codigo_cnpj | VARCHAR(14) NOT NULL | |
| codigo_item | VARCHAR(20) NOT NULL | FK lógica → item |
| codigo_deposito | VARCHAR(20) NOT NULL | FK lógica → deposito |
| quantidade_total | INT NOT NULL DEFAULT 0 | |
| quantidade_reservada | INT NOT NULL DEFAULT 0 | |
| quantidade_locada | INT NOT NULL DEFAULT 0 | |
| quantidade_manutencao | INT NOT NULL DEFAULT 0 | |
| created_at, updated_at | Auditoria | |

**Disponível (calculado):** total - reservada - locada - manutencao  
**UK:** (codigo_cnpj, codigo_item, codigo_deposito)

---

#### equipamento_patrimonial
Unidades patrimoniais (quando `item.controle_patrimonial = true`).

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| id | BIGSERIAL | PK |
| codigo_cnpj | VARCHAR(14) NOT NULL | |
| codigo_equipamento | VARCHAR(20) NOT NULL | Gerado pelo codigo_contador |
| codigo_item | VARCHAR(20) NOT NULL | FK lógica → item |
| codigo_deposito | VARCHAR(20) | Depósito atual (pode ser NULL se LOCADO) |
| status | VARCHAR(20) NOT NULL | DISPONIVEL, RESERVADO, LOCADO, MANUTENCAO, INATIVO |
| numero_serie | VARCHAR(100) | |
| inativo_em | TIMESTAMP | Nunca delete |
| created_at, created_by, updated_at, updated_by | Auditoria | |

**UK:** (codigo_cnpj, codigo_equipamento)

---

### 2.4 Contratos

#### contrato
Contrato de locação. Número gerado somente na entrega (PENDENTE → LOCADO).

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| id | BIGSERIAL | PK |
| codigo_cnpj | VARCHAR(14) NOT NULL | |
| numero_contrato | VARCHAR(50) | Formato ANO-CODIGO_CNPJ-SEQUENCIAL (ex: 2026-02-000157); NULL até LOCADO |
| codigo_cliente | VARCHAR(20) NOT NULL | FK lógica → cliente |
| codigo_endereco | VARCHAR(20) | Endereço de entrega |
| status | VARCHAR(20) NOT NULL | PENDENTE, LOCADO, RENOVADO, DEVOLVIDO, VENCIDO, CANCELADO |
| data_entrega | DATE | Preenchido ao passar para LOCADO |
| data_devolucao_prevista | DATE | |
| data_devolucao_efetiva | DATE | Preenchido na devolução |
| valor_total | DECIMAL(15,2) | |
| observacoes | TEXT | |
| created_at, created_by, updated_at, updated_by | Auditoria | |

---

#### contrato_periodo
Períodos do contrato (para renovações, cada renovação pode gerar novo período).

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| id | BIGSERIAL | PK |
| codigo_cnpj | VARCHAR(14) NOT NULL | |
| contrato_id | BIGINT NOT NULL | FK → contrato |
| data_inicio | DATE NOT NULL | |
| data_fim | DATE NOT NULL | |
| valor_periodo | DECIMAL(15,2) | |
| created_at, updated_at | Auditoria | |

---

#### contrato_periodo_item
Itens de cada período do contrato.

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| id | BIGSERIAL | PK |
| codigo_cnpj | VARCHAR(14) NOT NULL | |
| contrato_periodo_id | BIGINT NOT NULL | FK → contrato_periodo |
| codigo_item | VARCHAR(20) NOT NULL | FK lógica → item |
| codigo_equipamento | VARCHAR(20) | NULL se não patrimonial; FK lógica → equipamento_patrimonial |
| quantidade | INT NOT NULL DEFAULT 1 | |
| valor_unitario | DECIMAL(15,2) NOT NULL | |
| valor_total | DECIMAL(15,2) NOT NULL | |
| codigo_deposito_origem | VARCHAR(20) | Depósito de onde saiu |
| created_at, updated_at | Auditoria | |

---

#### contrato_evento
Timeline/auditoria de ações no contrato.

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| id | BIGSERIAL | PK |
| codigo_cnpj | VARCHAR(14) NOT NULL | |
| contrato_id | BIGINT NOT NULL | FK → contrato |
| tipo_evento | VARCHAR(50) NOT NULL | CRIACAO, RESERVA, ENTREGA, RENOVACAO, DEVOLUCAO, CANCELAMENTO, ALTERACAO, etc. |
| descricao | TEXT | |
| dados_json | JSONB | Payload da mudança |
| created_at | TIMESTAMP NOT NULL | |
| created_by | BIGINT | FK → usuario |

---

### 2.5 Manutenção

#### manutencao
Registro de manutenção de equipamento.

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| id | BIGSERIAL | PK |
| codigo_cnpj | VARCHAR(14) NOT NULL | |
| numero_manutencao | VARCHAR(20) NOT NULL | Gerado pelo codigo_contador |
| tipo | VARCHAR(30) NOT NULL | PREVENTIVA, CORRETIVA_INTERNA, CORRETIVA_CLIENTE |
| codigo_equipamento | VARCHAR(20) NOT NULL | FK lógica → equipamento_patrimonial |
| contrato_id | BIGINT | NULL exceto CORRETIVA_CLIENTE |
| status | VARCHAR(20) NOT NULL | ABERTA, EM_ANDAMENTO, CONCLUIDA, CANCELADA |
| data_abertura | DATE NOT NULL | |
| data_conclusao | DATE | |
| custo | DECIMAL(15,2) | Despesa (PREVENTIVA, CORRETIVA_INTERNA) |
| fatura_id | BIGINT | FK → fatura (quando CORRETIVA_CLIENTE faturada) |
| descricao | TEXT | |
| created_at, created_by, updated_at, updated_by | Auditoria | |

---

### 2.6 Vendas

#### venda
Venda de EPIs, acessórios, peças ou equipamento de locação.

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| id | BIGSERIAL | PK |
| codigo_cnpj | VARCHAR(14) NOT NULL | |
| numero_venda | VARCHAR(20) NOT NULL | Gerado pelo codigo_contador |
| codigo_cliente | VARCHAR(20) NOT NULL | FK lógica → cliente |
| data_venda | DATE NOT NULL | |
| valor_total | DECIMAL(15,2) NOT NULL | |
| status | VARCHAR(20) NOT NULL | PENDENTE, CONFIRMADA, CANCELADA |
| fatura_id | BIGINT | FK → fatura (quando faturada) |
| created_at, created_by, updated_at, updated_by | Auditoria | |

---

#### venda_item
Itens da venda.

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| id | BIGSERIAL | PK |
| codigo_cnpj | VARCHAR(14) NOT NULL | |
| venda_id | BIGINT NOT NULL | FK → venda |
| codigo_item | VARCHAR(20) NOT NULL | FK lógica → item |
| codigo_equipamento | VARCHAR(20) | NULL para VENDA_PRODUTO; preenchido para VENDA_EQUIPAMENTO |
| tipo_linha | VARCHAR(30) NOT NULL | VENDA_PRODUTO, VENDA_EQUIPAMENTO |
| quantidade | INT NOT NULL DEFAULT 1 | |
| valor_unitario | DECIMAL(15,2) NOT NULL | |
| valor_total | DECIMAL(15,2) NOT NULL | |
| created_at, updated_at | Auditoria | |

---

### 2.7 Faturamento

#### fatura
Fatura que pode reunir locação, manutenção, venda, indenização, multa, ajuste.

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| id | BIGSERIAL | PK |
| codigo_cnpj | VARCHAR(14) NOT NULL | |
| numero_fatura | VARCHAR(20) NOT NULL | Gerado pelo codigo_contador |
| codigo_cliente | VARCHAR(20) NOT NULL | FK lógica → cliente |
| data_emissao | DATE NOT NULL | |
| data_vencimento | DATE NOT NULL | |
| valor_total | DECIMAL(15,2) NOT NULL | |
| valor_pago | DECIMAL(15,2) NOT NULL DEFAULT 0 | |
| status | VARCHAR(20) NOT NULL | ABERTA, PARCIAL, PAGA, CANCELADA, VENCIDA |
| contrato_id | BIGINT | FK → contrato (quando origem locação) |
| venda_id | BIGINT | FK → venda (quando origem venda) |
| manutencao_id | BIGINT | FK → manutencao (quando origem CORRETIVA_CLIENTE) |
| created_at, created_by, updated_at, updated_by | Auditoria | |

---

#### fatura_item
Linhas da fatura.

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| id | BIGSERIAL | PK |
| codigo_cnpj | VARCHAR(14) NOT NULL | |
| fatura_id | BIGINT NOT NULL | FK → fatura |
| tipo_item | VARCHAR(30) NOT NULL | LOCACAO, MANUTENCAO, VENDA_PRODUTO, VENDA_EQUIPAMENTO, INDENIZACAO, MULTA, AJUSTE |
| descricao | VARCHAR(255) | |
| referencia_id | BIGINT | contrato_periodo_item, manutencao, venda_item, etc. |
| valor | DECIMAL(15,2) NOT NULL | |
| created_at, updated_at | Auditoria | |

---

#### fatura_pagamento
Pagamentos/baixas parciais da fatura.

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| id | BIGSERIAL | PK |
| codigo_cnpj | VARCHAR(14) NOT NULL | |
| fatura_id | BIGINT NOT NULL | FK → fatura |
| conta_financeira_id | BIGINT NOT NULL | FK → conta_financeira |
| data_pagamento | DATE NOT NULL | |
| valor | DECIMAL(15,2) NOT NULL | |
| forma_pagamento | VARCHAR(30) | DINHEIRO, PIX, CARTAO, BOLETO, TRANSFERENCIA, etc. |
| lancamento_financeiro_id | BIGINT | FK → lancamento_financeiro (quando gerado) |
| created_at, created_by, updated_at, updated_by | Auditoria | |

---

### 2.8 Financeiro

#### conta_financeira
Caixa e contas bancárias.

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| id | BIGSERIAL | PK |
| codigo_cnpj | VARCHAR(14) NOT NULL | |
| codigo_conta | VARCHAR(20) NOT NULL | |
| descricao | VARCHAR(255) NOT NULL | |
| tipo | VARCHAR(20) NOT NULL | CAIXA, BANCO |
| ativo | BOOLEAN DEFAULT true | |
| saldo_inicial | DECIMAL(15,2) DEFAULT 0 | |
| created_at, created_by, updated_at, updated_by | Auditoria | |

**UK:** (codigo_cnpj, codigo_conta)

---

#### categoria_financeira
Plano de contas / categorias para lançamentos.

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| id | BIGSERIAL | PK |
| codigo_cnpj | VARCHAR(14) NOT NULL | |
| codigo_categoria | VARCHAR(30) NOT NULL | |
| descricao | VARCHAR(255) NOT NULL | |
| tipo | VARCHAR(20) NOT NULL | RECEITA, DESPESA |
| categoria_pai_id | BIGINT | FK → categoria_financeira (hierarquia) |
| ativo | BOOLEAN DEFAULT true | |
| created_at, created_by, updated_at, updated_by | Auditoria | |

**UK:** (codigo_cnpj, codigo_categoria)

---

#### lancamento_financeiro
Lançamentos financeiros (entradas/saídas).

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| id | BIGSERIAL | PK |
| codigo_cnpj | VARCHAR(14) NOT NULL | |
| conta_financeira_id | BIGINT NOT NULL | FK → conta_financeira |
| categoria_financeira_id | BIGINT | FK → categoria_financeira |
| data_lancamento | DATE NOT NULL | |
| valor | DECIMAL(15,2) NOT NULL | Positivo=entrada, negativo=saída |
| tipo | VARCHAR(20) NOT NULL | ENTRADA, SAIDA |
| descricao | VARCHAR(255) | |
| fatura_pagamento_id | BIGINT | FK → fatura_pagamento (origem) |
| referencia_tipo | VARCHAR(50) | fatura, manutencao, venda, etc. |
| referencia_id | BIGINT | ID da entidade de origem |
| created_at, created_by, updated_at, updated_by | Auditoria | |

---

## 3. Diagrama de Relacionamentos (Resumo)

```
empresa (codigo_cnpj)
    │
    ├── usuario (codigo_cnpj)
    ├── codigo_contador (codigo_cnpj)
    ├── cliente (codigo_cnpj) ──┬── endereco (codigo_cliente)
    │                          │
    ├── item (codigo_cnpj)     │
    ├── deposito               │
    │   └── estoque_quantitativo (item, deposito)
    │   └── equipamento_patrimonial (item, deposito)
    │
    ├── contrato (codigo_cliente, codigo_endereco)
    │   ├── contrato_periodo
    │   │   └── contrato_periodo_item (item, equipamento)
    │   └── contrato_evento
    │
    ├── manutencao (codigo_equipamento, contrato?)
    │   └── fatura (quando CORRETIVA_CLIENTE)
    │
    ├── venda (codigo_cliente)
    │   └── venda_item (item, equipamento?)
    │
    ├── fatura (codigo_cliente, contrato?, venda?, manutencao?)
    │   ├── fatura_item
    │   └── fatura_pagamento → lancamento_financeiro
    │
    ├── conta_financeira
    ├── categoria_financeira
    └── lancamento_financeiro (conta, categoria, fatura_pagamento?)
```

---

## 4. Regras de Integridade e Isolamento

| Regra | Implementação |
|-------|---------------|
| Isolamento CNPJ | Todas as consultas/relatórios filtram por `codigo_cnpj` |
| Sem relacionamento cruzado | FKs e joins sempre no mesmo CNPJ |
| Cliente com histórico | Não apagar; `inativo_em` |
| Item/Equipamento com histórico | Não apagar; `inativo_em` |
| Contrato com renovação/devolução/faturamento | Não cancelar direto; estornar cascatas antes |
| Fatura com pagamento | Não cancelar direto; estornar pagamentos e lançamentos primeiro |
| codigo_cnpj imutável | Constraint UNIQUE em empresa; nunca UPDATE |

---

## 5. Próximos Passos (ENTREGA B)

Ao solicitar, serão gerados:

- Migrations Flyway (PostgreSQL) para todas as tabelas acima
- Constraints (PK, FK, UK, CHECK) e índices essenciais
- Enums (PostgreSQL e/ou Java) e mapeamento JPA

---

*Documento gerado como ENTREGA A — Modelo de Dados.*
