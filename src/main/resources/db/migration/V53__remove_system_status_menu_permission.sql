-- O menu SYSTEM_STATUS foi removido da enumeração AppMenuItem. As permissões
-- existentes precisam sair antes de o Hibernate converter menu_key para enum,
-- caso contrário a tela /funcoes-usuarios responde 500.
DELETE FROM user_role_menu_permission
WHERE menu_key = 'SYSTEM_STATUS';
