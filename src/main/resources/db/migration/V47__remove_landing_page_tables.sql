-- =============================================================================
-- V47 — Remove tabelas legadas da landing page
-- O modulo de landing page foi removido do projeto principal.
-- =============================================================================

DROP TABLE IF EXISTS lp_step CASCADE;
DROP TABLE IF EXISTS lp_beneficio CASCADE;
DROP TABLE IF EXISTS lp_faq CASCADE;
DROP TABLE IF EXISTS lp_plano CASCADE;
DROP TABLE IF EXISTS lp_testemunho CASCADE;
DROP TABLE IF EXISTS lp_config CASCADE;
