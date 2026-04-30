UPDATE usuario_sistema
SET role = 'ADMIN'
WHERE role = 'ADMINISTRATIVO';

CREATE TABLE user_role_menu_permission (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role VARCHAR(30) NOT NULL,
    menu_key VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_role_menu_permission UNIQUE (role, menu_key)
);
