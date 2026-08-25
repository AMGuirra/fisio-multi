// Lista centralizada de rotas cobertas pela auditoria de responsividade.
// Adicione uma rota nova aqui e ela entra automaticamente em todas as
// checagens (overflow, zoom, orientacao).

export const AUTHENTICATED_ROUTES = [
    { name: 'dashboard', path: '/dashboard' },
    { name: 'agenda', path: '/calendario' },
    { name: 'solicitacoes', path: '/solicitacoes' },
    { name: 'pacientes', path: '/pacientes' },
    { name: 'planos', path: '/planos-consumo' },
    { name: 'prontuario', path: '/prontuario' },
    { name: 'financeiro', path: '/financeiro' },
    { name: 'contas-receber', path: '/financeiro/contas-a-receber' },
    { name: 'contas-pagar', path: '/financeiro/contas-a-pagar' },
    { name: 'relatorios', path: '/relatorios' },
    { name: 'config-clinica', path: '/configuracoes/clinica' },
    { name: 'bloqueios', path: '/bloqueios' },
    { name: 'usuarios', path: '/usuarios' },
    { name: 'auditoria', path: '/auditoria' },
];

// Rotas prioritarias com defeito ja documentado historicamente - sempre
// incluidas nas checagens de zoom e orientacao (subconjunto mais caro de
// rodar em todas as combinacoes).
export const PRIORITY_ROUTE_NAMES = [
    'auditoria',
    'pacientes',
    'financeiro',
    'relatorios',
    'contas-receber',
    'contas-pagar',
];

export const PUBLIC_ROUTES = [
    { name: 'login', path: '/login' },
    { name: 'agendamento-publico', path: '/agendamento-online' },
    { name: 'lista-espera-publica', path: '/lista-espera-publica' },
    { name: 'portal-login', path: '/paciente/login' },
    { name: 'portal-esqueci-senha', path: '/paciente/esqueci-minha-senha' },
    { name: 'portal-resetar-senha', path: '/paciente/resetar-senha' },
    // 'portal-home' (/paciente/{token}) e resolvida em tempo de execucao,
    // pois depende de um paciente existir no banco.
];

export const VIEWPORTS = [
    { name: '320x568', w: 320, h: 568 }, // iPhone SE
    { name: '375x667', w: 375, h: 667 }, // iPhone 8
    { name: '390x844', w: 390, h: 844 }, // iPhone 12/13/14
    { name: '430x932', w: 430, h: 932 }, // iPhone 14/15 Pro Max
    { name: '768x1024', w: 768, h: 1024 }, // iPad portrait
    { name: '820x1180', w: 820, h: 1180 }, // iPad Air portrait
    { name: '1024x768', w: 1024, h: 768 }, // iPad landscape / notebook pequeno
    { name: '1100x768', w: 1100, h: 768 }, // tela dividida do Windows
    { name: '1280x720', w: 1280, h: 720 },
    { name: '1366x768', w: 1366, h: 768 },
    { name: '1440x900', w: 1440, h: 900 },
    { name: '1920x1080', w: 1920, h: 1080 },
];

// Bases usadas para simular zoom do navegador (o Chromium/CDP nao expoe um
// "zoom" real de UI para Playwright; a tecnica padrao para testar reflow -
// WCAG 1.4.10 - e reduzir o viewport CSS proporcionalmente ao fator de zoom).
export const ZOOM_BASES = [
    { name: 'desktop-1280x800', w: 1280, h: 800 },
    { name: 'laptop-1366x768', w: 1366, h: 768 },
];
export const ZOOM_FACTORS = [1.0, 1.25, 1.5, 2.0];

export const ORIENTATION_DEVICES = [
    { name: 'iphone-portrait', w: 390, h: 844 },
    { name: 'iphone-landscape', w: 844, h: 390 },
    { name: 'ipad-portrait', w: 768, h: 1024 },
    { name: 'ipad-landscape', w: 1024, h: 768 },
];
