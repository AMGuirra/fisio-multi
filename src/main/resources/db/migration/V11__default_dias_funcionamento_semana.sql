UPDATE configuracao_clinica
SET dias_funcionamento = 'MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY',
    updated_at = CURRENT_TIMESTAMP
WHERE TRIM(COALESCE(dias_funcionamento, '')) = '';
