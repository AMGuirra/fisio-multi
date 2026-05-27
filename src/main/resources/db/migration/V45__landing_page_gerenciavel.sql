-- =============================================================================
-- V45 — Landing Page gerenciável
-- Cria tabelas para gerenciar o conteúdo da landing page via painel admin.
-- =============================================================================

-- Configurações escalares (chave → valor)
CREATE TABLE lp_config (
    chave      VARCHAR(100) PRIMARY KEY,
    valor      TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Depoimentos
CREATE TABLE lp_testemunho (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    nome        VARCHAR(160) NOT NULL,
    cargo       VARCHAR(200),
    depoimento  TEXT         NOT NULL,
    iniciais    VARCHAR(4)   NOT NULL,
    cor_avatar  SMALLINT     NOT NULL DEFAULT 1,
    estrelas    SMALLINT     NOT NULL DEFAULT 5,
    ordem       SMALLINT     NOT NULL DEFAULT 0,
    ativo       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Planos e preços
CREATE TABLE lp_plano (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    nome        VARCHAR(100)  NOT NULL,
    descricao   TEXT,
    preco       NUMERIC(10,2) NOT NULL DEFAULT 0,
    periodo     VARCHAR(30)   NOT NULL DEFAULT '/mês',
    recursos    TEXT,
    destaque    BOOLEAN       NOT NULL DEFAULT FALSE,
    badge_texto VARCHAR(60),
    cta_texto   VARCHAR(80)   NOT NULL DEFAULT 'Começar agora',
    cta_link    VARCHAR(200)  NOT NULL DEFAULT '/login',
    ordem       SMALLINT      NOT NULL DEFAULT 0,
    ativo       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- FAQ
CREATE TABLE lp_faq (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    pergunta   VARCHAR(500) NOT NULL,
    resposta   TEXT         NOT NULL,
    ordem      SMALLINT     NOT NULL DEFAULT 0,
    ativo      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Benefícios
CREATE TABLE lp_beneficio (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    icone      VARCHAR(30)  NOT NULL DEFAULT 'STAR',
    cor        SMALLINT     NOT NULL DEFAULT 1,
    titulo     VARCHAR(160) NOT NULL,
    descricao  TEXT,
    ordem      SMALLINT     NOT NULL DEFAULT 0,
    ativo      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Passos "Como funciona"
CREATE TABLE lp_step (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    numero     SMALLINT     NOT NULL,
    titulo     VARCHAR(160) NOT NULL,
    descricao  TEXT,
    ordem      SMALLINT     NOT NULL DEFAULT 0,
    ativo      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- =============================================================================
-- Dados padrão — espelham o conteúdo atual da landing page
-- =============================================================================

INSERT INTO lp_config (chave, valor) VALUES
    ('hero.badge',              'Software especializado em fisioterapia'),
    ('hero.titulo',             'Gestão clínica que trabalha com você'),
    ('hero.subtitulo',          'Agenda inteligente, prontuário integrado, portal do paciente e relatórios de ocupação em tempo real. Tudo que sua clínica precisa em um único sistema.'),
    ('hero.cta_primario',       'Ver funcionalidades'),
    ('hero.cta_primario_link',  '#funcionalidades'),
    ('hero.cta_secundario',     'Acessar o sistema'),
    ('hero.cta_secundario_link','/login'),
    ('hero.stat1_valor',        '360°'),
    ('hero.stat1_label',        'Visão da clínica'),
    ('hero.stat2_valor',        '24h'),
    ('hero.stat2_label',        'Lembretes automáticos'),
    ('hero.stat3_valor',        '100%'),
    ('hero.stat3_label',        'LGPD Compliant'),
    ('hero.stat4_valor',        '0'),
    ('hero.stat4_label',        'Sessões perdidas sem aviso'),
    ('secao.como_funciona',     'true'),
    ('secao.funcionalidades',   'true'),
    ('secao.beneficios',        'true'),
    ('secao.depoimentos',       'true'),
    ('secao.planos',            'true'),
    ('secao.faq',               'true')
ON CONFLICT (chave) DO NOTHING;

INSERT INTO lp_testemunho (nome, cargo, depoimento, iniciais, cor_avatar, estrelas, ordem) VALUES
    ('Mariana Pereira',
     'Fisioterapeuta · Clínica Movimento, São Paulo - SP',
     'Antes eu perdia sessões toda semana por esquecimento dos pacientes. Com os lembretes automáticos do FisioGestão, minha taxa de comparecimento subiu de 68% para 91% em dois meses. A agenda ficou muito mais previsível.',
     'MP', 1, 5, 1),
    ('Rafael Costa',
     'Fisioterapeuta · Fisio & Vida, Curitiba - PR',
     'O controle de planos e saldo de sessões era um caos na planilha. Hoje vejo em tempo real quantas sessões cada paciente tem, o sistema avisa quando está acabando e a renovação é automática. Economizo pelo menos duas horas por dia.',
     'RC', 2, 5, 2),
    ('Juliana Santos',
     'Fisioterapeuta · ReabilFisio, Belo Horizonte - MG',
     'O portal do paciente foi o grande diferencial. Meus pacientes adoram acompanhar a evolução da dor na escala EVA e ver o saldo de sessões. Recebi elogios de vários deles pela transparência e modernidade da clínica.',
     'JS', 3, 5, 3);

INSERT INTO lp_plano (nome, descricao, preco, periodo, recursos, destaque, badge_texto, cta_texto, cta_link, ordem) VALUES
    ('Básico',
     'Ideal para consultórios individuais que precisam de organização e controle.',
     97.00, '/mês',
     '+Agenda inteligente
+Até 50 pacientes ativos
+Lembretes automáticos por e-mail
+Portal do paciente
-Relatórios avançados
-Múltiplos profissionais',
     FALSE, NULL, 'Começar agora', '/login', 1),
    ('Profissional',
     'Para clínicas em crescimento que querem controle total e relatórios completos.',
     197.00, '/mês',
     '+Tudo do plano Básico
+Pacientes ilimitados
+Relatórios avançados e exportação
+Lista de espera inteligente
+Até 3 profissionais
+Painel financeiro completo',
     TRUE, 'Mais popular', 'Começar agora', '/login', 2),
    ('Clínica',
     'Para clínicas consolidadas com múltiplas salas, equipes e alta demanda.',
     347.00, '/mês',
     '+Tudo do plano Profissional
+Profissionais ilimitados
+Múltiplas salas e agendas
+Auditoria completa de acessos
+Suporte prioritário
+Onboarding personalizado',
     FALSE, NULL, 'Falar com a equipe', '/login', 3);

INSERT INTO lp_faq (pergunta, resposta, ordem) VALUES
    ('Preciso instalar algum software para usar o FisioGestão?',
     'Não. O FisioGestão é 100% baseado na web — funciona em qualquer navegador moderno, no computador, tablet ou celular. Sem instalação, sem atualizações manuais. Basta acessar e começar.',
     1),
    ('Os dados dos pacientes ficam seguros? O sistema é compatível com a LGPD?',
     'Sim. Todos os dados são criptografados em trânsito (HTTPS/TLS) e em repouso. O sistema conta com controle de acesso por perfil de usuário, trilha de auditoria completa de todas as ações e sessões com expiração automática. Está em conformidade com a Lei Geral de Proteção de Dados (LGPD).',
     2),
    ('Posso ter mais de um profissional acessando ao mesmo tempo?',
     'Sim, dependendo do plano. O plano Profissional suporta até 3 profissionais simultâneos com perfis e permissões individuais. O plano Clínica permite profissionais ilimitados, com controle granular de acesso por função.',
     3),
    ('Como funcionam os lembretes automáticos para os pacientes?',
     'O sistema envia automaticamente um e-mail de confirmação para o paciente 24 horas antes de cada sessão agendada. O e-mail contém data, horário e nome do profissional. Nenhuma ação manual é necessária.',
     4),
    ('Posso cancelar o plano a qualquer momento?',
     'Sim. Não há contrato de fidelidade nem multa por cancelamento. Você pode encerrar a assinatura a qualquer momento e continuar usando o sistema até o fim do período pago.',
     5),
    ('O portal do paciente é seguro? Ele acessa dados de outros pacientes?',
     'O portal do paciente é completamente isolado por autenticação individual. Cada paciente acessa apenas seus próprios dados — prontuário, saldo de sessões, escala EVA e histórico de agendamentos.',
     6);

INSERT INTO lp_beneficio (icone, cor, titulo, descricao, ordem) VALUES
    ('BELL',   1, 'Lembretes automáticos 24h antes',       'O sistema envia e-mails de confirmação automaticamente para os pacientes um dia antes da sessão. Zero faltas por esquecimento.',                                    1),
    ('LIST',   2, 'Lista de espera inteligente',           'Quando há cancelamento, o próximo da fila é notificado automaticamente. Slots vazios viram receita aproveitada.',                                                      2),
    ('CHART',  3, 'Relatórios de ocupação em tempo real',  'Taxa de ocupação, faltas faturadas, receita por período e ranking de absenteísmo. Decisões baseadas em dados, não intuição.',                                         3),
    ('CARD',   1, 'Gestão de planos e sessões',            'Controle o saldo de sessões de cada paciente, automatize renovações e vincule planos a agendamentos de forma transparente.',                                          4),
    ('MOBILE', 2, 'Portal do paciente mobile-first',       'O paciente consulta prontuário, acompanha evolução pela escala EVA, vê saldo de sessões e agenda online sem instalar nada.',                                         5),
    ('LOCK',   3, 'Dados seguros e conformidade LGPD',     'Autenticação segura, criptografia em repouso, trilha de auditoria completa e controle de acesso por perfil. Seus dados, protegidos.',                               6);

INSERT INTO lp_step (numero, titulo, descricao, ordem) VALUES
    (1, 'Crie sua conta',           'Acesse o sistema com seu e-mail profissional. Em segundos sua clínica já tem um perfil ativo e seguro.',                                                   1),
    (2, 'Configure sua clínica',    'Cadastre profissionais, horários e tipos de planos. Interface intuitiva, sem manuais.',                                                                    2),
    (3, 'Agenda pronta para uso',   'Adicione pacientes, crie planos e comece a agendar. Lembretes automáticos já ativos desde o primeiro agendamento.',                                        3),
    (4, 'Acompanhe os resultados',  'Painel em tempo real: ocupação, faltas faturadas, receita e evolução clínica. Gestão visual, zero planilha.',                                              4);
