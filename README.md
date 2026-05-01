# FisioGestao

Sistema monolítico em Spring Boot para gestão de clínica de fisioterapia com financeiro integrado no mesmo banco.

## Stack

- Java 25
- Spring Boot 3.5.12
- Spring MVC + Spring Security + Spring Data JPA
- Thymeleaf + Bootstrap 5
- PostgreSQL (produção)
- H2 (testes)

## Módulos disponíveis

- Dashboard operacional e financeiro
- Agenda de sessões e bloqueios de horários
- Solicitações de agendamento online
- Usuários persistidos em PostgreSQL
- Financeiro:
  - lançamento manual de receitas e despesas
  - baixa de lançamentos pendentes
  - geração automática de receita ao criar pacote de sessões

## Execução local

1. Ajuste a variável `JAVA_HOME` para a raiz do JDK (sem `\bin`):

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-25.0.2'
```

2. Configure o acesso ao PostgreSQL por variáveis de ambiente (ou ajuste `src/main/resources/application.yml`).

3. Suba a aplicação:

```powershell
.\mvnw.cmd spring-boot:run
```

Aplicação padrão: [http://localhost:8081](http://localhost:8081)

## Primeiro acesso

O sistema não cria mais o usuário inseguro `admin/admin` automaticamente.

Para inicializar o primeiro administrador em um ambiente vazio, configure as variáveis abaixo antes de subir a aplicação:

```powershell
$env:APP_BOOTSTRAP_ADMIN_ENABLED='true'
$env:APP_BOOTSTRAP_ADMIN_USERNAME='admin'
$env:APP_BOOTSTRAP_ADMIN_EMAIL='admin@sua-clinica.com'
$env:APP_BOOTSTRAP_ADMIN_PASSWORD='TroquePorUmaSenhaForte123'
```

O bootstrap é executado apenas quando não existe nenhum usuário cadastrado.

## Testes e validação

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-25.0.2'
.\mvnw.cmd test
.\mvnw.cmd -DskipTests compile
```

## Deploy no Render

- Em ambiente Render, prefira a `Internal Database URL` do Postgres quando o banco e o web service estiverem na mesma regiao.
- Mantenha `DATABASE_URL` ou `SPRING_DATASOURCE_*` configurados no proprio servico para apontar sempre para o mesmo PostgreSQL persistente.
- Configure `APP_BASE_URL` com o dominio publico final do servico no Render. Essa URL e usada em links de redefinicao de senha e acessos publicos.
- Defina `APP_ENCRYPTION_KEY` antes de salvar configuracoes SMTP. A troca dessa chave depois impede a leitura dos segredos ja criptografados.
- Para o primeiro acesso em ambiente vazio, configure `APP_BOOTSTRAP_ADMIN_*` apenas ate criar o primeiro usuario administrador.
- O arquivo `render.yaml` foi ajustado para nao sobrescrever automaticamente a conexao do banco em syncs futuros do Blueprint e agora usa nomes padrao coerentes com o FisioGestao.
- A aplicacao bloqueia o boot no Render se tentar subir sem uma configuracao explicita de banco ou apontando para `localhost`, evitando deploys em base errada ou vazia.

### Flyway checksum mismatch

Se o deploy falhar com `Migration checksum mismatch`, o banco ja aplicou migrations antigas e os arquivos locais dessas mesmas versoes mudaram depois. Para reconciliar o historico do Flyway no banco persistente:

1. Confirme que `DATABASE_URL` ou `SPRING_DATASOURCE_*` apontam para o PostgreSQL correto.
2. No Render, defina temporariamente `APP_FLYWAY_REPAIR_ON_STARTUP=true`.
3. Rode um novo deploy e aguarde a aplicacao subir com sucesso.
4. Remova a variavel ou volte para `APP_FLYWAY_REPAIR_ON_STARTUP=false` e rode outro deploy.

Use esse procedimento apenas quando a estrutura atual do banco ja estiver compatível com as migrations locais. Para novas alteracoes de schema, crie sempre uma nova migration versionada em vez de editar arquivos `V*.sql` ja aplicados.

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
  whatsapp/
  shared/
```
# Fisioterapia
