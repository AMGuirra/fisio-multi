INSERT INTO user_role_menu_permission (role, menu_key)
SELECT DISTINCT permissao.role, submenu.menu_key
  FROM user_role_menu_permission permissao
 CROSS JOIN (
        SELECT 'CONTAS_RECEBER' AS menu_key
        UNION ALL SELECT 'CONTAS_PAGAR'
        UNION ALL SELECT 'FATURAMENTO'
 ) submenu
 WHERE permissao.role <> 'ADMIN'
   AND permissao.menu_key = 'FINANCEIRO'
   AND NOT EXISTS (
        SELECT 1
          FROM user_role_menu_permission atual
         WHERE atual.role = permissao.role
           AND atual.menu_key = submenu.menu_key
   );
