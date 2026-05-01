# Render PostgreSQL

## Objetivo

Usar o banco PostgreSQL hospedado no Render como banco principal da aplicacao e, se necessario, migrar os dados do banco local para a nuvem.

## Configurar a aplicacao para usar o Render

Se voce estiver usando Blueprint, prefira o arquivo `render.yaml` na raiz do projeto. Ele ja define:

- um web service Docker
- um Postgres gerenciado pelo Render
- preservacao da configuracao de banco no painel do servico
- health check em `/actuator/health`
- variaveis para bootstrap do primeiro administrador sem gravar segredo no Git

Se os recursos ja existirem no seu workspace, ajuste os nomes no `render.yaml` para bater exatamente com os nomes atuais antes do primeiro sync.

Importante: um web service criado manualmente no painel do Render nao passa a usar `render.yaml` so porque o arquivo foi enviado ao GitHub. Nesse caso, voce precisa:

- criar ou conectar um Blueprint no dashboard do Render
- executar o primeiro sync do Blueprint

Se preferir manter o servico manual, defina no proprio dashboard uma destas opcoes:

- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- ou `DATABASE_URL`

Tambem configure obrigatoriamente:

- `APP_BASE_URL` com a URL publica final do servico, por exemplo `https://seu-servico.onrender.com`
- `APP_ENCRYPTION_KEY` com uma chave forte e estavel para criptografar credenciais SMTP

## Corrigir checksum mismatch do Flyway

Se o Render mostrar erro como `Migration checksum mismatch for migration version ...`, o banco persistente tem registros antigos em `flyway_schema_history_fisiogestao` e os arquivos `V*.sql` dessas mesmas versoes foram alterados no codigo.

Procedimento seguro para reconciliar:

1. Confira no servico Render que `DATABASE_URL` ou `SPRING_DATASOURCE_*` apontam para o banco PostgreSQL persistente correto.
2. Defina temporariamente `APP_FLYWAY_REPAIR_ON_STARTUP=true`.
3. Faca um novo deploy.
4. Depois que o boot concluir, remova a variavel ou defina `APP_FLYWAY_REPAIR_ON_STARTUP=false`.
5. Faca outro deploy para garantir que a aplicacao volta a subir sem executar `repair` sempre.

Nao deixe `APP_FLYWAY_REPAIR_ON_STARTUP=true` permanentemente. Para mudancas futuras no schema, adicione uma nova migration (`V20__...sql`, `V21__...sql`, etc.) em vez de editar migrations ja aplicadas.

Opcionalmente, para um ambiente novo sem usuarios:

- `APP_BOOTSTRAP_ADMIN_ENABLED=true`
- `APP_BOOTSTRAP_ADMIN_USERNAME`
- `APP_BOOTSTRAP_ADMIN_EMAIL`
- `APP_BOOTSTRAP_ADMIN_PASSWORD`

Copie `render-db.env.example` para `render-db.env` na raiz do projeto e preencha com a `External Database URL` do Render.

Voce pode usar qualquer um destes formatos:

```bash
DATABASE_URL=postgresql://USUARIO:SENHA@HOST_RENDER:5432/BANCO?sslmode=require
```

ou

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://HOST_RENDER:5432/BANCO?sslmode=require
SPRING_DATASOURCE_USERNAME=USUARIO
SPRING_DATASOURCE_PASSWORD=SENHA
```

ou

```bash
PGHOST=HOST_RENDER
PGPORT=5432
PGDATABASE=BANCO
PGUSER=USUARIO
PGPASSWORD=SENHA
```

Depois disso:

- `iniciar.bat` carrega `render-db.env` automaticamente
- a aplicacao passa a priorizar essas variaveis antes do fallback local

## Migrar o banco local para o Render

### Requisitos

- cliente PostgreSQL instalado localmente
- comandos `pg_dump` e `pg_restore` disponiveis no `PATH`
- arquivo `render-db.env` preenchido com os dados do Render

### Script pronto

Use:

```powershell
.\scripts\migrate-local-db-to-render.ps1
```

Por padrao, ele:

- exporta o banco local `fisioterapia`
- conecta em `localhost:5432`
- usa usuario local `postgres`
- restaura no banco configurado em `render-db.env`

### Parametros opcionais

```powershell
.\scripts\migrate-local-db-to-render.ps1 `
  -LocalHost localhost `
  -LocalPort 5432 `
  -LocalDatabase fisioterapia `
  -LocalUser postgres `
  -LocalPassword postgres `
  -EnvFile render-db.env `
  -DumpFile local-fisioterapia.dump
```

## Observacoes importantes

- Para rodar localmente a partir da sua maquina, use a `External Database URL` do Render.
- Para o web service no proprio Render, prefira a `Internal Database URL`.
- Para conexao externa, mantenha `sslmode=require`.
- Se o cliente PostgreSQL nao estiver instalado, o script vai falhar com uma mensagem explicita.
- Em deploys no Render, o sistema bloqueia startup se detectar banco ausente ou apontando para `localhost`.
