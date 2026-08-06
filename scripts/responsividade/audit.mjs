// Suite de auditoria visual de responsividade do FisioGestao.
//
// Sobe contra uma instancia ja rodando da aplicacao (nao inicia o servidor -
// isso e responsabilidade de quem chama, veja README.md) e usa Chromium
// headless via Playwright para checar, em rota x resolucao reais:
//   1. overflow horizontal genuino (nao so a metrica scrollWidth - tenta
//      rolar de fato com scrollTo, porque overflow:hidden ainda permite
//      rolagem programatica/touch mesmo sem barra visivel);
//   2. reflow em zoom simulado (125/150/200%);
//   3. comportamento em retrato/paisagem (mobile e tablet);
//   4. a pagina de erro 404 autenticada.
//
// Uso:
//   cd scripts/responsividade
//   npm install && npm run install-browser
//   BASE_URL=http://localhost:8080 ADMIN_USER=admin ADMIN_PASSWORD=... npm run audit
//
// Screenshots so sao salvos para combinacoes que falharem (SCREENSHOT_MODE=all
// para salvar sempre, util em revisao manual). Sai com codigo != 0 se algo falhar,
// para uso em CI.

import { chromium } from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import {
    AUTHENTICATED_ROUTES,
    PUBLIC_ROUTES,
    PRIORITY_ROUTE_NAMES,
    VIEWPORTS,
    ZOOM_BASES,
    ZOOM_FACTORS,
    ORIENTATION_DEVICES,
} from './routes.mjs';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

const BASE = process.env.BASE_URL || 'http://localhost:8080';
const ADMIN_USER = process.env.ADMIN_USER || 'admin';
const ADMIN_PASSWORD = process.env.ADMIN_PASSWORD || 'AdminTeste123';
const SCREENSHOT_MODE = process.env.SCREENSHOT_MODE || 'on-failure'; // 'on-failure' | 'all' | 'none'
const OUT_DIR = path.join(__dirname, 'out');
const SHOT_DIR = path.join(OUT_DIR, 'screenshots');

fs.mkdirSync(SHOT_DIR, { recursive: true });

async function login(browser) {
    const context = await browser.newContext({ viewport: { width: 1366, height: 900 } });
    const page = await context.newPage();
    await page.goto(`${BASE}/login`, { waitUntil: 'networkidle' });
    await page.fill('#username', ADMIN_USER);
    await page.fill('#password', ADMIN_PASSWORD);
    await Promise.all([
        page.waitForNavigation({ waitUntil: 'networkidle' }),
        page.click('button[type=submit]'),
    ]);
    const storageState = await context.storageState();
    await context.close();
    return storageState;
}

// Garante que exista ao menos 1 paciente, para poder testar a rota do
// Portal (/paciente/{token}), que depende de um token real do banco.
async function ensurePatientAndGetPortalHref(browser, storageState) {
    const context = await browser.newContext({ viewport: { width: 1366, height: 900 }, storageState });
    const page = await context.newPage();
    await page.goto(`${BASE}/pacientes`, { waitUntil: 'networkidle' });
    let href = await page.getAttribute('a.action-button.ghost', 'href').catch(() => null);
    if (!href) {
        await page.click('#openNewPacienteModal');
        await page.waitForTimeout(400);
        await page.fill('input[name="nomeCompleto"]', 'Paciente Auditoria Responsividade');
        await page.fill('input[name="telefone"]', '(67) 90000-0000');
        const submitBtn = await page.$('#newPacienteModal button[type="submit"]');
        await Promise.all([
            page.waitForNavigation({ waitUntil: 'networkidle' }).catch(() => {}),
            submitBtn.click({ force: true }),
        ]);
        await page.waitForTimeout(400);
        href = await page.getAttribute('a.action-button.ghost', 'href');
    }
    await context.close();
    return href;
}

async function checkOverflow(page) {
    const doc = await page.evaluate(() => {
        const html = document.documentElement;
        window.scrollTo(99999, 0);
        const scrolledX = window.scrollX;
        window.scrollTo(0, 0);
        let offenders = [];
        if (scrolledX > 1) {
            offenders = [...document.querySelectorAll('*')]
                .filter((el) => el.getBoundingClientRect().right > html.clientWidth + 1)
                .slice(0, 8)
                .map((el) => ({
                    tag: el.tagName,
                    cls: el.className && el.className.toString ? el.className.toString().slice(0, 80) : '',
                    right: Math.round(el.getBoundingClientRect().right),
                }));
        }
        return { scrollWidth: html.scrollWidth, clientWidth: html.clientWidth, scrolledX, offenders };
    });
    return doc;
}

async function runOverflowSweep(browser, storageState, routes) {
    const results = [];
    for (const route of routes) {
        for (const vp of VIEWPORTS) {
            const context = await browser.newContext({
                viewport: { width: vp.w, height: vp.h },
                storageState: route.noAuth ? undefined : storageState,
            });
            const page = await context.newPage();
            const consoleErrors = [];
            page.on('console', (m) => { if (m.type() === 'error') consoleErrors.push(m.text()); });
            page.on('pageerror', (e) => consoleErrors.push(String(e)));

            let status = null, overflow = null, error = null;
            try {
                const resp = await page.goto(`${BASE}${route.path}`, { waitUntil: 'networkidle', timeout: 20000 });
                status = resp ? resp.status() : null;
                await page.waitForTimeout(250);
                overflow = await checkOverflow(page);
            } catch (e) {
                error = String(e).slice(0, 300);
            }

            const failed = !!error || (overflow && overflow.scrolledX > 1);
            if (SCREENSHOT_MODE === 'all' || (SCREENSHOT_MODE === 'on-failure' && failed)) {
                await page.screenshot({ path: path.join(SHOT_DIR, `${route.name}__${vp.name}.png`) }).catch(() => {});
            }

            results.push({
                phase: 'overflow',
                route: route.name,
                path: route.path,
                viewport: vp.name,
                status,
                scrollWidth: overflow?.scrollWidth ?? null,
                clientWidth: overflow?.clientWidth ?? null,
                scrolledX: overflow?.scrolledX ?? null,
                offenders: overflow?.offenders ?? [],
                consoleErrors: consoleErrors.slice(0, 5),
                error,
                failed,
            });
            await context.close();
        }
    }
    return results;
}

async function runZoomSweep(browser, storageState, routes) {
    const results = [];
    for (const route of routes) {
        for (const base of ZOOM_BASES) {
            for (const zoom of ZOOM_FACTORS) {
                const w = Math.round(base.w / zoom);
                const h = Math.round(base.h / zoom);
                const context = await browser.newContext({ viewport: { width: w, height: h }, storageState });
                const page = await context.newPage();
                let overflow = null, error = null;
                try {
                    await page.goto(`${BASE}${route.path}`, { waitUntil: 'networkidle', timeout: 20000 });
                    await page.waitForTimeout(200);
                    overflow = await checkOverflow(page);
                } catch (e) {
                    error = String(e).slice(0, 300);
                }
                const failed = !!error || (overflow && overflow.scrolledX > 1);
                if (SCREENSHOT_MODE === 'all' || (SCREENSHOT_MODE === 'on-failure' && failed)) {
                    await page.screenshot({ path: path.join(SHOT_DIR, `zoom__${route.name}__${base.name}__${zoom}.png`) }).catch(() => {});
                }
                results.push({
                    phase: 'zoom',
                    route: route.name,
                    base: base.name,
                    zoom,
                    effectiveViewport: `${w}x${h}`,
                    scrolledX: overflow?.scrolledX ?? null,
                    error,
                    failed,
                });
                await context.close();
            }
        }
    }
    return results;
}

async function runOrientationSweep(browser, storageState, routes) {
    const results = [];
    for (const route of routes) {
        for (const dev of ORIENTATION_DEVICES) {
            const context = await browser.newContext({
                viewport: { width: dev.w, height: dev.h },
                storageState,
                isMobile: dev.name.startsWith('iphone'),
                hasTouch: true,
            });
            const page = await context.newPage();
            let overflow = null, error = null, hasToggle = null;
            try {
                await page.goto(`${BASE}${route.path}`, { waitUntil: 'networkidle', timeout: 20000 });
                await page.waitForTimeout(200);
                overflow = await checkOverflow(page);
                hasToggle = await page.evaluate(() => {
                    const el = document.querySelector('#sidebarToggle, .header-toggle, .sidebar-toggle-btn');
                    if (!el) return false;
                    const box = el.getBoundingClientRect();
                    return box.width > 0 && box.height > 0;
                });
            } catch (e) {
                error = String(e).slice(0, 300);
            }
            const failed = !!error || (overflow && overflow.scrolledX > 1) || hasToggle === false;
            if (SCREENSHOT_MODE === 'all' || (SCREENSHOT_MODE === 'on-failure' && failed)) {
                await page.screenshot({ path: path.join(SHOT_DIR, `orientation__${route.name}__${dev.name}.png`) }).catch(() => {});
            }
            results.push({ phase: 'orientation', route: route.name, device: dev.name, scrolledX: overflow?.scrolledX ?? null, hasToggle, error, failed });
            await context.close();
        }
    }
    return results;
}

async function runErrorPageCheck(browser, storageState) {
    const context = await browser.newContext({ viewport: { width: 375, height: 667 }, storageState });
    const page = await context.newPage();
    let status = null, title = null, overflow = null, error = null;
    try {
        const resp = await page.goto(`${BASE}/rota-autenticada-que-nao-existe-xyz`, { waitUntil: 'networkidle', timeout: 20000 });
        status = resp ? resp.status() : null;
        title = await page.title();
        overflow = await checkOverflow(page);
    } catch (e) {
        error = String(e).slice(0, 300);
    }
    const failed = !!error || status !== 404 || (overflow && overflow.scrolledX > 1);
    if (SCREENSHOT_MODE === 'all' || (SCREENSHOT_MODE === 'on-failure' && failed)) {
        await page.screenshot({ path: path.join(SHOT_DIR, 'error-404.png') }).catch(() => {});
    }
    await context.close();
    return [{ phase: 'error-page', status, title, scrolledX: overflow?.scrolledX ?? null, error, failed }];
}

(async () => {
    const browser = await chromium.launch();
    const storageState = await login(browser);
    const portalHref = await ensurePatientAndGetPortalHref(browser, storageState);

    const allRoutes = [
        ...AUTHENTICATED_ROUTES,
        ...PUBLIC_ROUTES.map((r) => ({ ...r, noAuth: true })),
        { name: 'portal-home', path: portalHref, noAuth: true },
    ];
    const priorityRoutes = AUTHENTICATED_ROUTES.filter((r) => PRIORITY_ROUTE_NAMES.includes(r.name));

    console.error(`Rotas na varredura de overflow: ${allRoutes.length}`);
    const overflowResults = await runOverflowSweep(browser, storageState, allRoutes);

    console.error(`Rotas prioritarias para zoom/orientacao: ${priorityRoutes.length}`);
    const zoomResults = await runZoomSweep(browser, storageState, priorityRoutes);
    const orientationResults = await runOrientationSweep(browser, storageState, priorityRoutes);
    const errorPageResults = await runErrorPageCheck(browser, storageState);

    const all = [...overflowResults, ...zoomResults, ...orientationResults, ...errorPageResults];
    fs.writeFileSync(path.join(OUT_DIR, 'results.json'), JSON.stringify(all, null, 2));

    const failures = all.filter((r) => r.failed);
    console.log(`TOTAL_CHECKS=${all.length} FAILURES=${failures.length}`);
    for (const f of failures) {
        console.log('FAIL:', JSON.stringify(f));
    }

    await browser.close();
    process.exit(failures.length > 0 ? 1 : 0);
})();
