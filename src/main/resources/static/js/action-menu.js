/**
 * Menus de ações em <details class="patient-action-menu">.
 * O popover abre com position: fixed, calculado a partir do botão, para não ser cortado por
 * containers com overflow (ex.: lista rolável de sessões da agenda, tabelas responsivas).
 */
(function () {
    'use strict';

    const MENU = 'details.patient-action-menu';
    const MARGEM = 6;

    function posicionar(menu) {
        const popover = menu.querySelector('.patient-action-popover');
        const gatilho = menu.querySelector('summary');
        if (!popover || !gatilho) {
            return;
        }
        const r = gatilho.getBoundingClientRect();
        popover.style.position = 'fixed';
        if (window.innerWidth <= 768) {
            popover.style.left = '12px';
            popover.style.right = '12px';
        } else {
            popover.style.left = 'auto';
            popover.style.right = Math.max(8, window.innerWidth - r.right) + 'px';
        }
        popover.style.top = (r.bottom + MARGEM) + 'px';
        const altura = popover.getBoundingClientRect().height;
        if (r.bottom + MARGEM + altura > window.innerHeight && r.top - MARGEM - altura > 0) {
            popover.style.top = (r.top - MARGEM - altura) + 'px';
        }
    }

    function fecharTodos(exceto) {
        document.querySelectorAll(MENU + '[open]').forEach(function (menu) {
            if (menu !== exceto) {
                menu.open = false;
            }
        });
    }

    // "toggle" não propaga; captura no document.
    document.addEventListener('toggle', function (event) {
        const menu = event.target;
        if (!(menu instanceof HTMLElement) || !menu.matches(MENU)) {
            return;
        }
        const popover = menu.querySelector('.patient-action-popover');
        if (menu.open) {
            fecharTodos(menu);
            posicionar(menu);
        } else if (popover) {
            popover.removeAttribute('style');
        }
    }, true);

    document.addEventListener('click', function (event) {
        if (!event.target.closest(MENU)) {
            fecharTodos(null);
        }
    });

    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape') {
            fecharTodos(null);
        }
    });

    // Com position: fixed o popover não acompanha a rolagem; fecha para não ficar solto na tela.
    window.addEventListener('scroll', function (event) {
        if (event.target instanceof Element && event.target.closest('.patient-action-popover')) {
            return;
        }
        fecharTodos(null);
    }, true);
    window.addEventListener('resize', function () {
        fecharTodos(null);
    });
})();
