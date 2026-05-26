INSERT INTO user_role_menu_permission (role, menu_key)
SELECT DISTINCT permissao.role, 'HOJE'
  FROM user_role_menu_permission permissao
 WHERE permissao.role <> 'ADMIN'
   AND NOT EXISTS (
        SELECT 1
          FROM user_role_menu_permission atual
         WHERE atual.role = permissao.role
           AND atual.menu_key = 'HOJE'
   );

INSERT INTO user_role_menu_permission (role, menu_key)
SELECT DISTINCT permissao.role, 'LISTA_ESPERA'
  FROM user_role_menu_permission permissao
 WHERE permissao.role <> 'ADMIN'
   AND permissao.menu_key = 'SOLICITACOES'
   AND NOT EXISTS (
        SELECT 1
          FROM user_role_menu_permission atual
         WHERE atual.role = permissao.role
           AND atual.menu_key = 'LISTA_ESPERA'
   );
