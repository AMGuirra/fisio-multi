-- Reforca a limpeza de roles e menus legados, mantendo a V27 intacta caso ela
-- ja tenha sido aplicada em algum ambiente.

UPDATE usuario_sistema
   SET role = 'ADMIN'
 WHERE role IN ('ADMINISTRADOR', 'ADMINISTRATIVO');

UPDATE usuario_sistema
   SET role = 'FISIOTERAPEUTA'
 WHERE role IN ('PROFISSIONAL', 'FISIO');

UPDATE usuario_sistema
   SET role = 'RECEPCAO'
 WHERE role IN ('RECEPCIONISTA', 'SECRETARIA');

UPDATE usuario_sistema
   SET role = 'ASSESSORIA'
 WHERE role = 'VISUALIZADOR';

UPDATE usuario_sistema
   SET role = 'RECEPCAO'
 WHERE role NOT IN ('ADMIN', 'FISIOTERAPEUTA', 'RECEPCAO', 'ASSESSORIA', 'FINANCEIRO');

DELETE FROM user_role_menu_permission a
USING user_role_menu_permission b
WHERE a.ctid > b.ctid
  AND a.menu_key = b.menu_key
  AND a.role IN ('ADMINISTRATIVO', 'ADMINISTRADOR')
  AND b.role IN ('ADMINISTRATIVO', 'ADMINISTRADOR');

DELETE FROM user_role_menu_permission legado
USING user_role_menu_permission atual
WHERE legado.menu_key = atual.menu_key
  AND legado.role IN ('ADMINISTRATIVO', 'ADMINISTRADOR')
  AND atual.role = 'ADMIN';

UPDATE user_role_menu_permission
   SET role = 'ADMIN'
 WHERE role IN ('ADMINISTRATIVO', 'ADMINISTRADOR');

DELETE FROM user_role_menu_permission a
USING user_role_menu_permission b
WHERE a.ctid > b.ctid
  AND a.menu_key = b.menu_key
  AND a.role IN ('PROFISSIONAL', 'FISIO')
  AND b.role IN ('PROFISSIONAL', 'FISIO');

DELETE FROM user_role_menu_permission legado
USING user_role_menu_permission atual
WHERE legado.menu_key = atual.menu_key
  AND legado.role IN ('PROFISSIONAL', 'FISIO')
  AND atual.role = 'FISIOTERAPEUTA';

UPDATE user_role_menu_permission
   SET role = 'FISIOTERAPEUTA'
 WHERE role IN ('PROFISSIONAL', 'FISIO');

DELETE FROM user_role_menu_permission a
USING user_role_menu_permission b
WHERE a.ctid > b.ctid
  AND a.menu_key = b.menu_key
  AND a.role IN ('RECEPCIONISTA', 'SECRETARIA')
  AND b.role IN ('RECEPCIONISTA', 'SECRETARIA');

DELETE FROM user_role_menu_permission legado
USING user_role_menu_permission atual
WHERE legado.menu_key = atual.menu_key
  AND legado.role IN ('RECEPCIONISTA', 'SECRETARIA')
  AND atual.role = 'RECEPCAO';

UPDATE user_role_menu_permission
   SET role = 'RECEPCAO'
 WHERE role IN ('RECEPCIONISTA', 'SECRETARIA');

DELETE FROM user_role_menu_permission legado
USING user_role_menu_permission atual
WHERE legado.menu_key = atual.menu_key
  AND legado.role = 'VISUALIZADOR'
  AND atual.role = 'ASSESSORIA';

UPDATE user_role_menu_permission
   SET role = 'ASSESSORIA'
 WHERE role = 'VISUALIZADOR';

DELETE FROM user_role_menu_permission a
USING user_role_menu_permission b
WHERE a.ctid > b.ctid
  AND a.role = b.role
  AND a.menu_key IN ('PACOTES', 'PACOTES_SESSOES', 'PACOTE_SESSAO', 'PLANOS')
  AND b.menu_key IN ('PACOTES', 'PACOTES_SESSOES', 'PACOTE_SESSAO', 'PLANOS');

DELETE FROM user_role_menu_permission legado
USING user_role_menu_permission atual
WHERE legado.role = atual.role
  AND legado.menu_key IN ('PACOTES', 'PACOTES_SESSOES', 'PACOTE_SESSAO', 'PLANOS')
  AND atual.menu_key = 'PLANOS_SESSOES';

UPDATE user_role_menu_permission
   SET menu_key = 'PLANOS_SESSOES'
 WHERE menu_key IN ('PACOTES', 'PACOTES_SESSOES', 'PACOTE_SESSAO', 'PLANOS');

DELETE FROM user_role_menu_permission a
USING user_role_menu_permission b
WHERE a.ctid > b.ctid
  AND a.role = b.role
  AND a.menu_key IN ('CONFIGURACOES', 'CONFIGURACAO_CLINICA', 'CLINICA')
  AND b.menu_key IN ('CONFIGURACOES', 'CONFIGURACAO_CLINICA', 'CLINICA');

DELETE FROM user_role_menu_permission legado
USING user_role_menu_permission atual
WHERE legado.role = atual.role
  AND legado.menu_key IN ('CONFIGURACOES', 'CONFIGURACAO_CLINICA', 'CLINICA')
  AND atual.menu_key = 'CONFIG_CLINICA';

UPDATE user_role_menu_permission
   SET menu_key = 'CONFIG_CLINICA'
 WHERE menu_key IN ('CONFIGURACOES', 'CONFIGURACAO_CLINICA', 'CLINICA');

DELETE FROM user_role_menu_permission a
USING user_role_menu_permission b
WHERE a.ctid > b.ctid
  AND a.role = b.role
  AND a.menu_key IN ('EMAIL', 'CONFIGURACAO_EMAIL')
  AND b.menu_key IN ('EMAIL', 'CONFIGURACAO_EMAIL');

DELETE FROM user_role_menu_permission legado
USING user_role_menu_permission atual
WHERE legado.role = atual.role
  AND legado.menu_key IN ('EMAIL', 'CONFIGURACAO_EMAIL')
  AND atual.menu_key = 'CONFIG_EMAIL';

UPDATE user_role_menu_permission
   SET menu_key = 'CONFIG_EMAIL'
 WHERE menu_key IN ('EMAIL', 'CONFIGURACAO_EMAIL');

DELETE FROM user_role_menu_permission a
USING user_role_menu_permission b
WHERE a.ctid > b.ctid
  AND a.role = b.role
  AND a.menu_key IN ('STATUS_SISTEMA', 'SYSTEM')
  AND b.menu_key IN ('STATUS_SISTEMA', 'SYSTEM');

DELETE FROM user_role_menu_permission legado
USING user_role_menu_permission atual
WHERE legado.role = atual.role
  AND legado.menu_key IN ('STATUS_SISTEMA', 'SYSTEM')
  AND atual.menu_key = 'SYSTEM_STATUS';

UPDATE user_role_menu_permission
   SET menu_key = 'SYSTEM_STATUS'
 WHERE menu_key IN ('STATUS_SISTEMA', 'SYSTEM');

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
WHERE a.ctid > b.ctid
  AND a.role = b.role
  AND a.menu_key = b.menu_key;
