(() => {
    const menus = [...document.querySelectorAll('.finance-action-menu')];
    const paymentModal = document.getElementById('financePaymentModal');
    const paymentForm = document.getElementById('financePaymentModalForm');
    const paymentMethod = document.getElementById('financePaymentMethod');

    if (!menus.length || !paymentModal || !paymentForm || !paymentMethod) return;

    let openMenu = null;
    let lastPaymentTrigger = null;

    const syncModalState = () => {
        document.body.classList.toggle('modal-open', document.querySelector('.app-modal.is-open') !== null);
    };

    const positionMenu = (menu) => {
        const trigger = menu.querySelector('.finance-action-trigger');
        const popover = menu.querySelector('.finance-action-popover');
        if (!trigger || !popover) return;

        const rect = trigger.getBoundingClientRect();
        const margin = 8;
        const maxLeft = window.innerWidth - popover.offsetWidth - margin;
        const left = Math.max(margin, Math.min(rect.right - popover.offsetWidth, maxLeft));
        const below = rect.bottom + margin;
        const above = rect.top - popover.offsetHeight - margin;
        const top = below + popover.offsetHeight <= window.innerHeight || above < margin ? below : above;

        popover.style.left = `${left}px`;
        popover.style.top = `${top}px`;
    };

    const closeMenu = (menu = openMenu, restoreFocus = false) => {
        if (!menu) return;
        const trigger = menu.querySelector('.finance-action-trigger');
        const popover = menu.querySelector('.finance-action-popover');
        popover.hidden = true;
        trigger.setAttribute('aria-expanded', 'false');
        if (openMenu === menu) openMenu = null;
        if (restoreFocus) trigger.focus();
    };

    const openMenuFor = (menu) => {
        if (openMenu && openMenu !== menu) closeMenu(openMenu);
        const trigger = menu.querySelector('.finance-action-trigger');
        const popover = menu.querySelector('.finance-action-popover');
        popover.hidden = false;
        trigger.setAttribute('aria-expanded', 'true');
        openMenu = menu;
        positionMenu(menu);
        popover.querySelector('[role="menuitem"]')?.focus();
    };

    const closePaymentModal = () => {
        paymentModal.classList.remove('is-open');
        paymentModal.setAttribute('aria-hidden', 'true');
        syncModalState();
        lastPaymentTrigger?.focus();
        lastPaymentTrigger = null;
    };

    menus.forEach((menu) => {
        const trigger = menu.querySelector('.finance-action-trigger');
        const popover = menu.querySelector('.finance-action-popover');
        trigger.addEventListener('click', () => openMenu === menu ? closeMenu(menu) : openMenuFor(menu));

        popover.addEventListener('click', (event) => {
            const paymentTrigger = event.target.closest('.js-open-finance-payment-modal');
            if (paymentTrigger) {
                closeMenu(menu);
                lastPaymentTrigger = trigger;
                paymentForm.action = paymentTrigger.dataset.paymentUrl;
                paymentForm.reset();
                paymentModal.classList.add('is-open');
                paymentModal.setAttribute('aria-hidden', 'false');
                syncModalState();
                paymentMethod.focus();
                return;
            }
            if (event.target.closest('[role="menuitem"]')) closeMenu(menu);
        });
    });

    document.addEventListener('click', (event) => {
        if (openMenu && !openMenu.contains(event.target)) closeMenu();
    });

    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape') {
            if (paymentModal.classList.contains('is-open')) closePaymentModal();
            else if (openMenu) closeMenu(openMenu, true);
        }
    });

    window.addEventListener('resize', () => openMenu && positionMenu(openMenu));
    window.addEventListener('scroll', () => openMenu && positionMenu(openMenu), true);

    paymentModal.querySelectorAll('[data-finance-payment-close]').forEach((element) => {
        element.addEventListener('click', closePaymentModal);
    });
})();
