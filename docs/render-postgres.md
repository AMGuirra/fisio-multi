# Render PostgreSQL

## Objetivo

Usar o banco PostgreSQL hospedado no Render como banco principal da aplicacao e, se necessario, migrar os dados do banco local para a nuvem.

## Configurar a aplicacao para usar o Render

Se voce estiver usando Blueprint, prefira o arquivo `render.yaml` na raiz do projeto. Ele ja define:

- um web service Docker
- um Postgres gerenciado pelo Render
- vinculacao automatica da `DATABASE_URL`
- health check em `/actuator/health`

Se os recursos ja existirem no seu workspace, ajuste os nomes no `render.yaml` para bater exatamente com os nomes atuais antes do primeiro sync.

Importante: um web service criado manualmente no painel do Render nao passa a usar `render.yaml` so porque o arquivo foi enviado ao GitHub. Nesse caso, voce precisa:

- criar ou conectar um Blueprint no dashboard do Render
- executar o primeiro sync do Blueprint

Se preferir manter o servico manual, defina no proprio dashboard uma destas opcoes:

- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- ou `DATABASE_URL`

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
- `startup-serv.bat` carrega `render-db.env` automaticamente
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

- exporta o banco local `financeiro`
- conecta em `localhost:5432`
- usa usuario local `postgres`
- restaura no banco configurado em `render-db.env`

### Parametros opcionais

```powershell
.\scripts\migrate-local-db-to-render.ps1 `
  -LocalHost localhost `
  -LocalPort 5432 `
  -LocalDatabase financeiro `
  -LocalUser postgres `
  -LocalPassword postgres `
  -EnvFile render-db.env `
  -DumpFile financeiro.dump
```

## Observacoes importantes

- Para rodar localmente a partir da sua maquina, use a `External Database URL` do Render.
- A `Internal Database URL` serve para servicos rodando dentro do proprio Render.
- Para conexao externa, mantenha `sslmode=require`.
- Se o cliente PostgreSQL nao estiver instalado, o script vai falhar com uma mensagem explicita.
