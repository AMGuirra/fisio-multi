ALTER TABLE sessao
    ADD COLUMN pacote_consumo_registrado BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE sessao
SET pacote_consumo_registrado = TRUE
WHERE pacote_sessao_id IS NOT NULL
  AND status = 'REALIZADA';
