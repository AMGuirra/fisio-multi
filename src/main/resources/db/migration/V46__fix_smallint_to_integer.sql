-- =============================================================================
-- V46 — Corrige tipo SMALLINT → INTEGER nas tabelas da landing page
-- Hibernate 6 valida o tipo exato: int Java = INTEGER (int4), não SMALLINT (int2).
-- =============================================================================

ALTER TABLE lp_testemunho
    ALTER COLUMN cor_avatar TYPE INTEGER,
    ALTER COLUMN estrelas   TYPE INTEGER,
    ALTER COLUMN ordem      TYPE INTEGER;

ALTER TABLE lp_plano
    ALTER COLUMN ordem TYPE INTEGER;

ALTER TABLE lp_faq
    ALTER COLUMN ordem TYPE INTEGER;

ALTER TABLE lp_beneficio
    ALTER COLUMN cor   TYPE INTEGER,
    ALTER COLUMN ordem TYPE INTEGER;

ALTER TABLE lp_step
    ALTER COLUMN numero TYPE INTEGER,
    ALTER COLUMN ordem  TYPE INTEGER;
