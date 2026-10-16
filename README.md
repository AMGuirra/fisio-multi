# FisioGestao

Sistema monolítico em Spring Boot para gestão de clínicas de fisioterapia, operando como **SaaS multiempresa**:
várias clínicas usam a mesma aplicação e o mesmo banco PostgreSQL, com isolamento total de dados entre elas.

## Stack

- Java 26
- Spring Boot 3.5.14
- Spring MVC + Spring Security + Spring Data JPA (Hibernate 6.6)
- Thymeleaf + Bootstrap 5
- PostgreSQL + Flyway
- H2 em testes unitários e smoke tests

## Módulos disponíveis

- Dashboard operacional e financeiro
- Agenda de sessões, bloqueios e solicitações de agendamento online
- Pacientes, planos de tratamento, prontuário e anexos
- Financeiro (lançamentos, contas a pagar/receber, recibos) e relatórios
- Portal do paciente e lista de espera
- Usuários, funções/permissões de menu, auditoria e configuração de e-mail
- **Gestor da plataforma** (superadmin): empresas, período de teste, suporte, cancelamentos e configurações globais

## Execução local

Padrões locais (já configurados em `src/main/resources/application.yml`):

| Item | Valor |
|------|-------|
| URL | http://localhost:8080 |
| PostgreSQL | `localhost:5432`, banco `fisio` |
| Usuário / senha do banco | `postgres` / `postgres` |
| JDBC | `jdbc:postgresql://localhost:5432/fisio` |

### Scripts para Windows (recomendado)

Na pasta `bat/` (funcionam também se chamados de outro diretório). Todos rodam somente localmente, sem git e sem o banco do Render:

| Script | O que faz |
|--------|-----------|
| `bat/iniciar-segundo-plano.bat` | Inicia o sistema em segundo plano (janela oculta), sem depender do terminal aberto. Compila se ainda não houver JAR. Logs em `logs/` |
| `bat/atualizar.bat` | Aplica as últimas alterações do código: encerra a instância, recompila e inicia de novo em segundo plano |
| `bat/parar-local.bat` | Mostra o PID que está usando a porta 8080 e pede confirmação antes de encerrá-lo (não toca em outros processos) |

Depois de iniciar, acesse http://localhost:8080 com `admin` / `admin` (código da clínica em branco).

Observações:

- O banco `fisio` precisa existir antes (veja "Execução manual", passo 1). As tabelas são criadas e atualizadas pelo Flyway na inicialização.
- A senha padrão do PostgreSQL usada pelos scripts é `postgres`. Para outra senha, defina antes:
  `set FISIO_DB_PASSWORD=sua_senha` (CMD) ou `$env:FISIO_DB_PASSWORD='sua_senha'` (PowerShell). A senha não é exibida.
- Os scripts forçam `jdbc:postgresql://localhost:5432/fisio`, porta `8080` e `APP_BASE_URL=http://localhost:8080`,
  e limpam (só naquele processo) variáveis de produção como `DATABASE_URL` e `RENDER`.
- O superadmin `admin/admin` só é criado se ainda não existir; uma senha alterada depois não é sobrescrita.

### Execução manual

1. Crie o banco (uma única vez), se ainda não existir:

```powershell
& 'C:\Program Files\PostgreSQL\16\bin\psql.exe' -h localhost -U postgres -c "CREATE DATABASE fisio"
```

2. Aponte o `JAVA_HOME` para o JDK 26 (raiz, sem `\bin`) e suba a aplicação. O Flyway cria/atualiza o schema sozinho:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-26.0.2.1'
.\mvnw.cmd spring-boot:run
```

3. Acesse http://localhost:8080.

### Credenciais locais

| Perfil | Código da clínica | Usuário | Senha |
|--------|-------------------|---------|-------|
| SUPER_ADMIN (área Gestor) | *(em branco)* | `admin` | `admin` |

> **`admin/admin` é exclusivo para uso local.** O bootstrap só cria esse usuário quando
> `app.bootstrap-super-admin.enabled=true` (padrão local) e **nunca** sobrescreve a senha de um usuário
> existente. No perfil `prod` (ou no Render) o bootstrap fica desligado e a senha `admin` é recusada mesmo
> se ele for ligado. Troque a senha após o primeiro acesso se o ambiente for compartilhado.

### Variáveis de ambiente (produção)

Todas as configurações locais podem ser sobrescritas:

| Variável | Uso |
|----------|-----|
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` (ou `DATABASE_URL`) | Banco PostgreSQL |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_SSLMODE` | Alternativa para montar a URL local |
| `PORT` / `SERVER_PORT` | Porta HTTP (padrão 8080) |
| `APP_BASE_URL` | URL pública usada em links de e-mail |
| `APP_ENCRYPTION_KEY` | Chave para criptografar senhas SMTP |
| `APP_SECURITY_REMEMBER_ME_KEY` | Chave do "lembrar-me" (obrigatória em produção) |
| `APP_BOOTSTRAP_SUPER_ADMIN_ENABLED`, `APP_BOOTSTRAP_SUPER_ADMIN_USERNAME`, `APP_BOOTSTRAP_SUPER_ADMIN_PASSWORD` | Criação do primeiro superadmin em produção (use senha forte e desligue depois) |
| `APP_TENANT_SLUG_PUBLICO_PADRAO` | Clínica usada por links públicos antigos sem `?clinica=` (padrão `clinica-principal`; vazio desativa) |

## Multiempresa (tenancy)

- **Modelo:** tenancy por coluna (`empresa_id`) no mesmo schema. A tabela `empresa` é o tenant.
- **Origem do tenant:** sempre o usuário autenticado (empresa resolvida no login a partir do banco). Nenhum
  `empresa_id` enviado pelo navegador é considerado. Rotas públicas resolvem a clínica pelo **slug**
  (`?clinica=...`) ou por **token** já vinculado a ela (portal do paciente, redefinição de senha).
- **Isolamento automático:** as entidades de clínica herdam `TenantEntity` (`@TenantId` do Hibernate). O
  Hibernate filtra JPQL, Criteria/Specifications e `findById`, preenche `empresa_id` nos inserts e rejeita
  gravação com empresa divergente. Sem empresa no contexto, as consultas não retornam nada (falha fechada).
- **SQL nativo / JDBC:** filtra explicitamente por `empresa_id` (dashboard, contagens, vínculo de sessões).
- **Usuários, auditoria e e-mail:** filtrados explicitamente (o superadmin e eventos da plataforma têm
  `empresa_id` nulo).
- **Banco:** FKs compostas `(id, empresa_id)` e gatilhos impedem vincular registros de empresas diferentes;
  unicidades passaram a ser por empresa (username, e-mail de usuário, CPF de paciente, permissões, SMTP ativo).
- **Jobs agendados:** executados empresa por empresa, cada uma no seu escopo e fuso horário.
- **Dados legados:** a migração V56 vincula tudo o que já existia à empresa **"Clínica Principal"**
  (slug `clinica-principal`). O antigo papel `ADMIN` virou `ADMIN_CLINICA`.

### Login

A tela de login tem o campo **Código da clínica** (slug). Deixe em branco para o superadmin. Usuários de
clínica podem entrar sem o código quando o usuário/e-mail + senha identificam uma única conta; se a mesma
credencial existir em mais de uma clínica, o código é obrigatório. O navegador lembra o último código usado.
Usuários existentes da clínica legada entram com o código `clinica-principal`.

### Papéis

- `SUPER_ADMIN`: global, sem empresa; acessa apenas `/gestor/**` e o SMTP global (`/configuracoes/email`).
  Não acessa páginas operacionais de clínicas e não há impersonação.
- `ADMIN_CLINICA`: administrador da própria clínica (acesso total à clínica).
- `FISIOTERAPEUTA`, `RECEPCAO`, `ASSESSORIA`, `FINANCEIRO`: menus configuráveis por clínica em "Funções".

## Status da empresa e acesso

| Status | Acesso |
|--------|--------|
| `ATIVO` | Liberado por tempo indeterminado |
| `TESTE` | Liberado até a data/hora final do teste (23:59:59 do último dia útil) |
| `BLOQUEADO` | Negado — "O acesso da sua clínica está bloqueado." |
| `INATIVO` | Negado — "A clínica está inativa." |
| `TESTE` expirado | Negado — "O período de teste da sua clínica foi finalizado." |

A regra é verificada no login (após a senha conferir, para não revelar status a terceiros) **e em toda
requisição autenticada**: se o status mudar, a sessão existente é invalidada na requisição seguinte e o
usuário vê a tela `/acesso-negado` com botão para o suporte. Nenhum dado é apagado ao bloquear/inativar.

## Período de teste (dias úteis)

- Calculado só por `DiasUteisService`: ignora sábados, domingos e feriados nacionais (01/01, Sexta-feira
  Santa, 21/04, 01/05, 07/09, 12/10, 02/11, 15/11, 20/11 desde 2024 e 25/12). Carnaval e Corpus Christi
  são pontos facultativos e não são descontados.
- Cadastro público: status `TESTE` com a quantidade padrão de dias úteis (inicialmente **15**), configurável
  em **Gestor → Configurações globais**.
- No Gestor (detalhe da empresa): iniciar/reiniciar teste com N dias úteis, prorrogar (+N), definir o total
  (aumentar ou reduzir, recalculando a data final a partir do início) ou definir uma data final manual
  (validada; a quantidade de dias úteis é recalculada). Tudo é auditado.
- Rotina diária (08:00): avisa o e-mail administrativo e a clínica quando faltam N dias úteis (padrão 3,
  configurável) e quando o teste expira; também conclui cancelamentos agendados.

## Cadastro de nova clínica

Página pública **`/cadastro`** (link na tela de login): nome da clínica/fisioterapeuta, responsável, CPF e/ou
CNPJ (validados), e-mail, telefone, senha, aceite dos termos e observações. Ao concluir:

- cria a empresa em `TESTE`, um slug único (código de acesso) e a configuração inicial da clínica;
- cria o primeiro usuário `ADMIN_CLINICA` (login pelo e-mail ou pelo usuário exibido);
- libera o acesso imediatamente e mostra as instruções de login;
- registra auditoria, cria alerta no Gestor e envia e-mail ao endereço administrativo global.

Proteções: CSRF, validação de entrada, rate limiting (5 cadastros/30 min por IP) e mensagem genérica para
duplicidade de CNPJ (não revela cadastros existentes).

## Área Gestor (SUPER_ADMIN)

Menu **Gestor** visível apenas para o superadmin:

- **Painel:** totais por status, novos cadastros no mês, testes vencendo/expirados, suporte e cancelamentos
  pendentes, alertas.
- **Empresas:** busca por nome, código, CPF/CNPJ ou e-mail, filtro por status, paginação; cadastro e edição;
  ações com confirmação: ativar/desbloquear, bloquear e inativar (motivo obrigatório), iniciar/reiniciar,
  prorrogar e alterar teste; equipe da empresa (somente dados administrativos — sem dados clínicos).
- **Cancelamentos:** filtros por status, empresa, data e responsável; histórico; colocar em análise, recusar,
  agendar e concluir (dupla confirmação). Concluir torna a empresa `INATIVO`, encerra as sessões e preserva os
  dados. Para reativar, use **Ativar** ou **Iniciar teste** (com os dias úteis desejados) na empresa.
- **Suporte:** solicitações públicas com busca, filtro e atualização de status
  (`PENDENTE`, `EM_ANALISE`, `RESPONDIDA`, `ENCERRADA`).
- **Auditoria global** e **Configurações globais** (dias de teste padrão, antecedência do aviso e e-mail
  administrativo — persistidos no banco).

### Fluxo de bloqueio/inativação

1. Gestor → Empresas → abrir a empresa → **Bloquear** ou **Inativar** e informar o motivo.
2. Os usuários da clínica perdem o acesso na próxima requisição e veem a mensagem correspondente.
3. A clínica pode abrir uma solicitação em `/suporte/solicitar`.
4. Para liberar novamente: **Ativar / desbloquear** (ou **Iniciar teste**).

### Cancelamento solicitado pela clínica

- Dentro do sistema: **Config. Clínica → Solicitar cancelamento** (somente `ADMIN_CLINICA`).
- Sem acesso ao sistema: página pública `/suporte/cancelamento` (empresa identificada pelo CNPJ/CPF quando
  possível, sem revelar se existe).
- A solicitação não muda o status nem apaga dados; gera alerta no Gestor, e-mail administrativo e auditoria.

## Suporte público

`/suporte/solicitar` (link nas telas de login e de acesso negado): nome, CPF e/ou CNPJ, e-mail e descrição
(até 500 caracteres). Protegido por CSRF, validação e rate limiting; salva a solicitação, vincula a empresa
quando o documento a identifica e notifica o e-mail administrativo.

## E-mail (SMTP) e notificações administrativas

- **SMTP da clínica:** cada clínica cadastra o próprio em **Config. E-mail** (o SMTP existente antes da
  migração ficou vinculado à Clínica Principal).
- **SMTP global da plataforma:** o superadmin cadastra em `/configuracoes/email` (link em Configurações
  globais). Ele envia as notificações administrativas e serve de fallback para clínicas sem SMTP próprio.
- **E-mail administrativo:** Gestor → Configurações globais → "E-mail administrativo para notificações".
  Recebe: novo cadastro, solicitação de suporte, solicitação de cancelamento, teste perto do fim, teste
  expirado e mudanças de status.
- Falhas de envio são registradas no log da aplicação e no log de e-mail, sem interromper o fluxo.
- Defina `APP_ENCRYPTION_KEY` antes de salvar senhas SMTP (local: qualquer valor estável; produção: aleatório
  e longo). Para Gmail, use uma senha de app com `smtp.gmail.com:587` e TLS.

## Rotas públicas por clínica

| Recurso | Como identifica a clínica |
|---------|---------------------------|
| Agendamento online | `/agendamento-online?clinica=<codigo>` |
| Horários disponíveis | `/api/slots?data=AAAA-MM-DD&clinica=<codigo>` |
| Login/recuperação do portal do paciente | `/paciente/login?clinica=<codigo>` |
| Portal do paciente | token do paciente (`/paciente/<token>`) |
| Redefinição de senha do paciente/usuário | token do e-mail |

Links sem `?clinica=` usam `APP_TENANT_SLUG_PUBLICO_PADRAO` (Clínica Principal) para não quebrar links antigos.
Clínicas sem acesso liberado não expõem páginas públicas.

## Migrations Flyway adicionadas

| Versão | Conteúdo |
|--------|----------|
| V54 | `empresa`, empresa legada "Clínica Principal", `configuracao_plataforma` |
| V55 | `solicitacao_suporte`, `solicitacao_cancelamento` (+ histórico), `notificacao_gestor` |
| V56 | `empresa_id` em todas as tabelas de negócio, migração dos dados para a Clínica Principal, FKs e índices |
| V57 | Papéis (`ADMIN` → `ADMIN_CLINICA`, `SUPER_ADMIN`), unicidades por empresa, FKs compostas, gatilhos e view de BI por empresa |

Nunca altere migrations já aplicadas: crie uma nova versão.

## Testes e validação

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-26.0.2.1'
.\mvnw.cmd test
```

O teste de migração real (`MigracaoMultiempresaPostgresTest`) roda quando houver PostgreSQL disponível; ele
cria e remove um banco temporário próprio:

```powershell
$env:FISIO_PG_TEST_URL = 'jdbc:postgresql://localhost:5432/postgres'
$env:FISIO_PG_TEST_USER = 'postgres'
$env:FISIO_PG_TEST_PASSWORD = 'postgres'
.\mvnw.cmd test
```

## Deploy no Render

- Em ambiente Render, prefira a `Internal Database URL` do Postgres quando o banco e o web service estiverem na mesma região.
- Mantenha `DATABASE_URL` ou `SPRING_DATASOURCE_*` configurados no próprio serviço para apontar sempre para o mesmo PostgreSQL persistente.
- Configure `APP_BASE_URL` com o domínio público final. Essa URL é usada em links de e-mail e acessos públicos.
- Defina `APP_ENCRYPTION_KEY` antes de salvar configurações SMTP. A troca dessa chave depois impede a leitura dos segredos já criptografados.
- Para criar o primeiro superadmin em produção, defina `APP_BOOTSTRAP_SUPER_ADMIN_ENABLED=true` e `APP_BOOTSTRAP_SUPER_ADMIN_PASSWORD` (forte) apenas no primeiro deploy.
- O arquivo `render.yaml` usa `sync: false` nas variáveis sensíveis para não sobrescrever a conexão do banco em syncs futuros do Blueprint.
- A aplicação bloqueia o boot no Render se tentar subir sem configuração explícita de banco ou apontando para `localhost`.
- Em plataformas gerenciadas, a aplicação respeita a variável `PORT`.

### Flyway checksum mismatch

Se o deploy falhar com `Migration checksum mismatch`, o banco já aplicou migrations antigas e os arquivos locais dessas mesmas versões mudaram depois. Para reconciliar o histórico do Flyway no banco persistente:

1. Confirme que `DATABASE_URL` ou `SPRING_DATASOURCE_*` apontam para o PostgreSQL correto.
2. No Render, defina temporariamente `APP_FLYWAY_REPAIR_ON_STARTUP=true`.
3. Rode um novo deploy e aguarde a aplicação subir com sucesso.
4. Remova a variável ou volte para `APP_FLYWAY_REPAIR_ON_STARTUP=false` e rode outro deploy.

## Estrutura principal

```text
src/main/java/com/fisiogestao/
  agenda/  auth/  auditoria/  cadastro/  cancelamento/  config/  dashboard/
  email/  empresa/  financeiro/  gestor/  listaespera/  pacientes/  portal/
  prontuario/  relatorios/  shared/  solicitacoes/  suporte/  tenant/  whatsapp/
```
