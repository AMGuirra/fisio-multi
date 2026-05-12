# FisioGestao

Sistema monolitico em Spring Boot para gestao de clinica de fisioterapia com financeiro integrado no mesmo banco.

## Stack

- Java 26
- Spring Boot 3.5.14
- Spring MVC + Spring Security + Spring Data JPA
- Thymeleaf + Bootstrap 5
- PostgreSQL em producao
- H2 em testes unitarios e smoke tests

## Modulos disponiveis

- Dashboard operacional e financeiro
- Agenda de sessoes e bloqueios de horarios
- Solicitacoes de agendamento online
- Usuarios persistidos em PostgreSQL
- Financeiro:
  - lancamento manual de receitas e despesas
  - baixa de lancamentos pendentes
  - geracao automatica de receita ao criar pacote de sessoes

## Execucao local

1. Ajuste a variavel `JAVA_HOME` para a raiz do JDK 26, sem `\bin`:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-26.0.1'
```

2. Configure o acesso ao PostgreSQL por variaveis de ambiente ou ajuste `src/main/resources/application.yml`.

3. Suba a aplicacao:

```powershell
.\mvnw.cmd spring-boot:run
```

Aplicacao padrao: [http://localhost:8081](http://localhost:8081)

## Primeiro acesso

O sistema nao cria mais o usuario inseguro `admin/admin` automaticamente.

Para inicializar o primeiro administrador em um ambiente vazio, configure as variaveis abaixo antes de subir a aplicacao:

```powershell
$env:APP_BOOTSTRAP_ADMIN_ENABLED='true'
$env:APP_BOOTSTRAP_ADMIN_USERNAME='admin'
$env:APP_BOOTSTRAP_ADMIN_EMAIL='admin@sua-clinica.com'
$env:APP_BOOTSTRAP_ADMIN_PASSWORD='TroquePorUmaSenhaForte123'
```

O bootstrap e executado apenas quando nao existe nenhum usuario cadastrado.

## Testes e validacao

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-26.0.1'
.\mvnw.cmd test
.\mvnw.cmd -DskipTests compile
```

## Deploy no Render

- Em ambiente Render, prefira a `Internal Database URL` do Postgres quando o banco e o web service estiverem na mesma regiao.
- Mantenha `DATABASE_URL` ou `SPRING_DATASOURCE_*` configurados no proprio servico para apontar sempre para o mesmo PostgreSQL persistente.
- Configure `APP_BASE_URL` com o dominio publico final do servico no Render. Essa URL e usada em links de redefinicao de senha e acessos publicos.
- Defina `APP_ENCRYPTION_KEY` antes de salvar configuracoes SMTP. A troca dessa chave depois impede a leitura dos segredos ja criptografados.
- Para o primeiro acesso em ambiente vazio, configure `APP_BOOTSTRAP_ADMIN_*` apenas ate criar o primeiro usuario administrador.
- O arquivo `render.yaml` usa `sync: false` nas variaveis sensiveis para nao sobrescrever a conexao do banco em syncs futuros do Blueprint.
- A aplicacao bloqueia o boot no Render se tentar subir sem configuracao explicita de banco ou apontando para `localhost`, evitando deploys em base errada ou vazia.
- A imagem Docker usa Java 26 e expoe a porta padrao `8081`; em plataformas gerenciadas, a aplicacao respeita a variavel `PORT`.

### Flyway checksum mismatch

Se o deploy falhar com `Migration checksum mismatch`, o banco ja aplicou migrations antigas e os arquivos locais dessas mesmas versoes mudaram depois. Para reconciliar o historico do Flyway no banco persistente:

1. Confirme que `DATABASE_URL` ou `SPRING_DATASOURCE_*` apontam para o PostgreSQL correto.
2. No Render, defina temporariamente `APP_FLYWAY_REPAIR_ON_STARTUP=true`.
3. Rode um novo deploy e aguarde a aplicacao subir com sucesso.
4. Remova a variavel ou volte para `APP_FLYWAY_REPAIR_ON_STARTUP=false` e rode outro deploy.

Use esse procedimento apenas quando a estrutura atual do banco ja estiver compativel com as migrations locais. Para novas alteracoes de schema, crie sempre uma nova migration versionada em vez de editar arquivos `V*.sql` ja aplicados.

## Estrutura principal

```text
src/main/java/com/fisiogestao/
  agenda/
  auth/
  config/
  dashboard/
  financeiro/
  pacientes/
  prontuario/
  solicitacoes/
  shared/
```
