# FisioGestao

Sistema monolítico em Spring Boot para gestão de clínica de fisioterapia com financeiro integrado no mesmo banco.

## Stack

- Java 17
- Spring Boot 3.4
- Spring MVC + Spring Security + Spring Data JPA
- Thymeleaf + Bootstrap 5
- PostgreSQL (produção)
- H2 (testes)

## Módulos disponíveis

- Dashboard operacional e financeiro
- Agenda de sessões e bloqueios de horários
- Solicitações de agendamento online
- Usuários (catálogo em memória para acesso local)
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

Aplicação padrão: [http://localhost:8080](http://localhost:8080)

## Testes e validação

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-25.0.2'
.\mvnw.cmd test
.\mvnw.cmd -DskipTests compile
```

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
