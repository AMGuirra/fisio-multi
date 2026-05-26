INSERT INTO user_role_menu_permission (role, menu_key)
SELECT DISTINCT role, 'BLOQUEIOS'
  FROM user_role_menu_permission origem
 WHERE origem.menu_key IN ('AGENDA', 'CONFIG_CLINICA')
   AND origem.role IN ('FISIOTERAPEUTA', 'RECEPCAO', 'ADMIN')
   AND NOT EXISTS (
       SELECT 1
         FROM user_role_menu_permission atual
        WHERE atual.role = origem.role
          AND atual.menu_key = 'BLOQUEIOS'
   );
