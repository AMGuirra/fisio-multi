## Scripts auxiliares

- Seed opcional por perfil (nao automatico):
  - `docs/sql/seed_demo_v18.sql`
  - `docs/sql/seed_hml_v18.sql`
  - `docs/sql/seed_treinamento_v18.sql`
- Checklist de validacao manual pos-implementacao:
  - `docs/checklists/checklist_pos_v18.md`

### Como executar os seeds opcionais
1. Garanta que as migracoes Flyway foram aplicadas normalmente.
2. Escolha um perfil e execute apenas um arquivo:
   - Demo: `docs/sql/seed_demo_v18.sql`
   - HML: `docs/sql/seed_hml_v18.sql`
   - Treinamento: `docs/sql/seed_treinamento_v18.sql`
3. Os scripts sao idempotentes para os dados principais (usa verificacoes e `ON CONFLICT`).
4. Se quiser aplicar mais de um perfil no mesmo banco, revise os codigos de cliente e e-mails para evitar mistura de cenarios.
