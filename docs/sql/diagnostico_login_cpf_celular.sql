-- Consultas de diagnóstico usadas antes de habilitar o login do Portal do Paciente por
-- CPF/celular (migração V49__normaliza_cpf_telefone_paciente.sql).
--
-- Rode estas consultas em uma cópia/réplica de produção antes de aplicar a migração, para
-- entender o estado real dos dados. Nenhuma delas altera dados — são somente leitura.

-- 1) Celular duplicado: pacientes ativos diferentes com o mesmo celular normalizado.
--    Quem aparecer aqui não poderá logar por celular sem ambiguidade (o sistema já detecta
--    isso em tempo de execução e orienta a usar CPF), mas vale revisar manualmente.
SELECT telefone, COUNT(*) AS quantidade, array_agg(id) AS pacientes
FROM cliente
WHERE ativo = true AND telefone IS NOT NULL AND telefone <> ''
GROUP BY telefone
HAVING COUNT(*) > 1
ORDER BY quantidade DESC;

-- 2) CPF duplicado (não deveria ocorrer: já existe uk_cliente_cpf, mas serve como checagem).
SELECT cpf, COUNT(*) AS quantidade, array_agg(id) AS pacientes
FROM cliente
WHERE ativo = true AND cpf IS NOT NULL AND cpf <> ''
GROUP BY cpf
HAVING COUNT(*) > 1
ORDER BY quantidade DESC;

-- 3) CPF com máscara ou caracteres não numéricos (deve zerar após a V49, mas útil para
--    conferir antes/depois de aplicar a migração).
SELECT id, cpf
FROM cliente
WHERE cpf IS NOT NULL AND cpf ~ '\D';

-- 4) Telefone com máscara ou caracteres não numéricos.
SELECT id, telefone
FROM cliente
WHERE telefone IS NOT NULL AND telefone ~ '\D';

-- 5) CPF com tamanho inválido (diferente de 11 dígitos) após normalização.
SELECT id, cpf, length(regexp_replace(cpf, '\D', '', 'g')) AS tamanho
FROM cliente
WHERE cpf IS NOT NULL
  AND length(regexp_replace(cpf, '\D', '', 'g')) <> 11;

-- 6) Telefone com tamanho inválido (fora de 10/11 dígitos) após normalização.
SELECT id, telefone, length(regexp_replace(telefone, '\D', '', 'g')) AS tamanho
FROM cliente
WHERE telefone IS NOT NULL
  AND length(regexp_replace(telefone, '\D', '', 'g')) NOT IN (10, 11);

-- 7) Pacientes sem CPF e sem celular: não conseguirão logar no portal por nenhum dos dois
--    novos métodos (só restará o fluxo assistido pela clínica / e-mail, se houver).
SELECT id, nome_completo, email
FROM cliente
WHERE ativo = true
  AND (cpf IS NULL OR cpf = '')
  AND (telefone IS NULL OR telefone = '');
