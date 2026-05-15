-- Normaliza dados legados que podem quebrar a conversao de enums do Hibernate
-- ao abrir a tela /usuarios, que tambem carrega as permissoes por funcao.

UPDATE usuario_sistema
   SET role = 'ADMIN'
 WHERE role = 'ADMINISTRATIVO';

DELETE FROM user_role_menu_permission legado
USING user_role_menu_permission atual
WHERE legado.menu_key = atual.menu_key
  AND legado.role = 'ADMINISTRATIVO'
  AND atual.role = 'ADMIN';

UPDATE user_role_menu_permission
   SET role = 'ADMIN'
 WHERE role = 'ADMINISTRATIVO';

DELETE FROM user_role_menu_permission legado
USING user_role_menu_permission atual
WHERE legado.role = atual.role
  AND legado.menu_key = 'CALENDARIO'
  AND atual.menu_key = 'AGENDA';

UPDATE user_role_menu_permission
   SET menu_key = 'AGENDA'
 WHERE menu_key = 'CALENDARIO';

DELETE FROM user_role_menu_permission legado
USING user_role_menu_permission atual
WHERE legado.role = atual.role
  AND legado.menu_key = 'BLOQUEIOS'
  AND atual.menu_key = 'CONFIG_CLINICA';

UPDATE user_role_menu_permission
   SET menu_key = 'CONFIG_CLINICA'
 WHERE menu_key = 'BLOQUEIOS';

DELETE FROM user_role_menu_permission
 WHERE role NOT IN ('ADMIN', 'FISIOTERAPEUTA', 'RECEPCAO', 'ASSESSORIA', 'FINANCEIRO')
    OR menu_key NOT IN (
        'DASHBOARD',
        'AGENDA',
        'CALENDARIO',
        'BLOQUEIOS',
        'SOLICITACOES',
        'PACIENTES',
        'PLANOS_SESSOES',
        'PRONTUARIO',
        'FINANCEIRO',
        'RELATORIOS',
        'CONFIG_CLINICA',
        'SYSTEM_STATUS',
        'USUARIOS',
        'FUNCOES_USUARIOS',
        'CONFIG_EMAIL',
        'AUDITORIA',
        'AGENDAMENTO_ONLINE'
    );

DELETE FROM user_role_menu_permission a
USING user_role_menu_permission b
WHERE a.created_at > b.created_at
  AND a.role = b.role
  AND a.menu_key = b.menu_key;
