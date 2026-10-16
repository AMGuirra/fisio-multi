-- Corrige a solicitacao rejeitada mais recente de cada paciente que ja esta
-- aguardando na lista de espera. Novas entradas passam a usar EM_ESPERA direto.
WITH solicitacoes_na_fila AS (
    SELECT DISTINCT ON (s.empresa_id, s.cliente_id)
           s.id
      FROM solicitacao_agendamento s
      JOIN lista_espera l
        ON l.empresa_id = s.empresa_id
       AND l.cliente_id = s.cliente_id
       AND l.status = 'AGUARDANDO'
     WHERE s.status = 'REJEITADA'
       AND s.cliente_id IS NOT NULL
     ORDER BY s.empresa_id, s.cliente_id, s.updated_at DESC
)
UPDATE solicitacao_agendamento s
   SET status = 'EM_ESPERA',
       updated_at = CURRENT_TIMESTAMP
  FROM solicitacoes_na_fila f
 WHERE s.id = f.id;
