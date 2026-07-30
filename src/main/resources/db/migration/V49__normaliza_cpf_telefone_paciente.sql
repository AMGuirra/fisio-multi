-- Normaliza CPF/telefone legados do paciente e cria índice para o novo login por celular.
--
-- Escopo (ver docs/sql/diagnostico_login_cpf_celular.sql para as consultas de análise
-- executadas antes desta migração):
--   1. Remove caracteres não numéricos de cliente.cpf e cliente.telefone nas linhas que
--      ainda tiverem máscara (a aplicação já salva só dígitos desde o início; isto é apenas
--      uma rede de segurança para dados legados/importados fora do fluxo normal).
--   2. Cria índice não-único em cliente.telefone para suportar a busca por celular no login
--      e a checagem de ambiguidade (mais de um paciente com o mesmo celular).
--
-- Deliberadamente NÃO cria restrição única em telefone: pacientes existentes podem ter o
-- mesmo celular cadastrado (ex.: familiares que usam o telefone de um dos dois). Enquanto
-- essa duplicidade não for tratada manualmente clínica a clínica, o login por celular detecta
-- a ambiguidade em tempo de execução e orienta o paciente a usar o CPF (ver
-- PacienteService#autenticarPortalPorCelular). Uma unique constraint só deve ser adicionada em
-- uma migração futura, depois de uma rodada de deduplicação.
--
-- Rollback manual (o Flyway Community não reverte migrações automaticamente):
--   DROP INDEX IF EXISTS idx_cliente_telefone;
--   -- A normalização de dígitos não precisa ser revertida: o valor sem máscara representa o
--   -- mesmo CPF/telefone, apenas em outro formato de armazenamento.

UPDATE cliente
SET cpf = regexp_replace(cpf, '\D', '', 'g')
WHERE cpf IS NOT NULL AND cpf ~ '\D';

UPDATE cliente
SET telefone = regexp_replace(telefone, '\D', '', 'g')
WHERE telefone IS NOT NULL AND telefone ~ '\D';

CREATE INDEX IF NOT EXISTS idx_cliente_telefone
    ON cliente (telefone)
    WHERE telefone IS NOT NULL;
