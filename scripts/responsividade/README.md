# Auditoria de responsividade (Playwright)

Suite automatizada que valida overflow horizontal real, reflow em zoom e
comportamento em retrato/paisagem contra uma instância rodando do FisioGestão,
usando Chromium headless. Criada durante a auditoria de responsividade do
sistema (ver histórico de commits a partir de "Corrige responsividade
estrutural...").

Diferente de checar só `document.documentElement.scrollWidth >
document.documentElement.clientWidth`, a suíte também tenta rolar de fato
(`window.scrollTo` e mede `window.scrollX` depois) — `overflow-x: hidden`
ainda permite rolagem programática/touch mesmo sem barra visível, então a
métrica sozinha pode dar falso negativo.

## Rotas e viewports cobertos

Lista centralizada em [`routes.mjs`](./routes.mjs):

- **Overflow horizontal**: todas as rotas autenticadas + públicas, em 12
  resoluções (320×568 até 1920×1080, incluindo larguras de tela dividida do
  Windows como 1024×768/1100×768).
- **Zoom simulado (100/125/150/200%)** e **orientação retrato/paisagem**
  (iPhone e iPad): só nas rotas marcadas em `PRIORITY_ROUTE_NAMES` (as que já
  tiveram defeito documentado — Auditoria, Pacientes, Financeiro, Relatórios,
  Contas a Receber/Pagar), para manter a suíte rápida.
- **Página de erro 404** autenticada.

Para adicionar uma rota nova à cobertura, edite `routes.mjs` — não precisa
tocar em `audit.mjs`.

## Como rodar localmente

1. Suba a aplicação apontando para um banco de teste (H2 em memória, sem
   precisar de Postgres) usando o classpath de testes do Maven, que já traz
   um admin bootstrap (`admin` / `AdminTeste123`, ver
   `src/test/resources/application.yml`):

   ```bash
   ./mvnw spring-boot:test-run
   ```

   A app sobe em `http://localhost:8080`.

2. Em outro terminal, instale as dependências e o Chromium (primeira vez
   apenas):

   ```bash
   cd scripts/responsividade
   npm install
   npm run install-browser
   ```

3. Rode a auditoria:

   ```bash
   npm run audit
   ```

   Variáveis de ambiente opcionais:

   | Variável | Padrão | Uso |
   |---|---|---|
   | `BASE_URL` | `http://localhost:8080` | onde a app está rodando |
   | `ADMIN_USER` / `ADMIN_PASSWORD` | `admin` / `AdminTeste123` | credenciais de login usadas pela suíte |
   | `SCREENSHOT_MODE` | `on-failure` | `on-failure` (só salva quando algo falha), `all` (salva tudo — útil para revisão manual), `none` |

4. Resultado: sai com código `0` se nada falhou, `1` caso contrário — dá para
   plugar em CI. `out/results.json` tem o detalhe de cada checagem;
   `out/screenshots/` tem as capturas (conforme `SCREENSHOT_MODE`). Esses
   arquivos são gerados e não versionados (ver `.gitignore`).

## O que a suíte **não** cobre

- Safari real (iOS/iPadOS) — o Chromium headless não reproduz motor de
  renderização nem particularidades de zoom/inputs do Safari. Use como
  validação complementar, não substituto, antes de mudanças sensíveis a
  `-webkit-*`/inputs nativos.
- Área mínima de toque (44×44px) medida individualmente por elemento — não
  há checagem automatizada disso ainda.
- Fluxos que exigem estado específico (ex.: consumo de sessões de um plano de
  tratamento, envio de anexos no prontuário) — a suíte só garante que a
  página carrega e não gera scroll horizontal, não testa cada interação.

## Rodando no CI (opcional)

Se for plugar num pipeline: suba a app com `spring-boot:test-run` em
background, faça polling em `/login` até responder, rode `npm run audit`
dentro de `scripts/responsividade`, e trate o exit code do script como
gate — sem precisar de nenhuma infra além de Node.
