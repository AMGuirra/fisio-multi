(() => {
    const body = document.body;
    const appSidebar = document.getElementById('appSidebar');
    const sidebarToggle = document.getElementById('sidebarToggle');
    const sidebarBackdrop = document.getElementById('sidebarBackdrop');
    const userMenu = document.getElementById('userMenu');
    const userMenuToggle = document.getElementById('userMenuToggle');
    const greetingElement = document.getElementById('dynamicGreeting');
    const headerDateElement = document.getElementById('headerFullDate');
    const infoDateElement = document.getElementById('infoDate');
    const liveClockElement = document.getElementById('liveClock');
    const infoTimeElement = document.getElementById('infoTime');
    const systemStatusElement = document.getElementById('systemStatus');

    const SIDEBAR_STORAGE_KEY = 'sidebarCollapsed';
    const MOBILE_BREAKPOINT = 920;
    const sidebarToggleHome = sidebarToggle ? document.createComment('sidebar-toggle-home') : null;

    if (sidebarToggle && sidebarToggle.parentNode && sidebarToggleHome) {
        sidebarToggle.parentNode.insertBefore(sidebarToggleHome, sidebarToggle);
    }

    const capitalize = (text) => {
        if (!text) {
            return '';
        }
        return text.charAt(0).toUpperCase() + text.slice(1);
    };

    const toFirstName = (name) => {
        if (!name || !name.trim()) {
            return 'Usuário';
        }
        return name.trim().split(/\s+/)[0];
    };

    const getGreeting = (hour) => {
        if (hour < 12) {
            return 'Bom dia';
        }
        if (hour < 18) {
            return 'Boa tarde';
        }
        return 'Boa noite';
    };

    const isMobileViewport = () => window.innerWidth <= MOBILE_BREAKPOINT;

    const readSidebarPreference = () => {
        try {
            return localStorage.getItem(SIDEBAR_STORAGE_KEY) === 'true';
        } catch (error) {
            return false;
        }
    };

    const saveSidebarPreference = (collapsed) => {
        try {
            localStorage.setItem(SIDEBAR_STORAGE_KEY, String(collapsed));
        } catch (error) {
            // Ignore storage failures to keep the interface functional.
        }
    };

    const updateDateTime = () => {
        const now = new Date();

        const fullDate = capitalize(now.toLocaleDateString('pt-BR', {
            weekday: 'long',
            day: '2-digit',
            month: 'long',
            year: 'numeric'
        }));

        const shortDate = now.toLocaleDateString('pt-BR', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric'
        });

        const timeWithSeconds = now.toLocaleTimeString('pt-BR', {
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit'
        });

        const timeNoSeconds = now.toLocaleTimeString('pt-BR', {
            hour: '2-digit',
            minute: '2-digit'
        });

        if (headerDateElement) {
            headerDateElement.textContent = fullDate;
        }

        if (infoDateElement) {
            infoDateElement.textContent = shortDate;
        }

        if (liveClockElement) {
            liveClockElement.textContent = timeNoSeconds;
        }

        if (infoTimeElement) {
            infoTimeElement.textContent = timeWithSeconds;
        }

        if (greetingElement) {
            const username = greetingElement.getAttribute('data-username') || 'Usuário';
            const firstName = toFirstName(username);
            greetingElement.textContent = `${getGreeting(now.getHours())}, ${firstName}`;
        }
    };

    const updateConnectionStatus = () => {
        if (!systemStatusElement) {
            return;
        }

        const online = navigator.onLine;
        systemStatusElement.classList.toggle('offline', !online);
        systemStatusElement.textContent = online ? 'Online' : 'Offline';
    };

    const closeUserMenu = () => {
        if (userMenu) {
            userMenu.classList.remove('open');
        }
    };

    const updateSidebarAccessibility = () => {
        const mobileViewport = isMobileViewport();
        const sidebarOpen = body.classList.contains('sidebar-open');
        const sidebarCollapsed = body.classList.contains('sidebar-collapsed');
        const sidebarExpanded = mobileViewport ? sidebarOpen : !sidebarCollapsed;
        const sidebarHidden = mobileViewport && !sidebarOpen;

        if (sidebarToggle) {
            sidebarToggle.setAttribute('aria-expanded', String(sidebarExpanded));
        }

        if (appSidebar) {
            appSidebar.setAttribute('aria-hidden', String(sidebarHidden));
        }

        if (sidebarBackdrop) {
            sidebarBackdrop.setAttribute('aria-hidden', String(!sidebarOpen));
            sidebarBackdrop.tabIndex = sidebarOpen ? 0 : -1;
        }
    };

    const closeMobileSidebar = () => {
        body.classList.remove('sidebar-open');
        updateSidebarAccessibility();
    };

    const relocateSidebarToggle = () => {
        if (!sidebarToggle || !appSidebar || !sidebarToggleHome) {
            return;
        }

        if (isMobileViewport()) {
            const homeParent = sidebarToggleHome.parentNode;
            if (homeParent && (sidebarToggle.parentNode !== homeParent || sidebarToggle.previousSibling !== sidebarToggleHome)) {
                homeParent.insertBefore(sidebarToggle, sidebarToggleHome.nextSibling);
            }
            return;
        }

        const sidebarNav = appSidebar.querySelector('.sidebar-nav');
        if (sidebarNav && (sidebarToggle.parentNode !== appSidebar || sidebarToggle.nextElementSibling !== sidebarNav)) {
            appSidebar.insertBefore(sidebarToggle, sidebarNav);
        }
    };

    const syncSidebarLayout = () => {
        if (isMobileViewport()) {
            body.classList.remove('sidebar-collapsed');
        } else {
            body.classList.remove('sidebar-open');
            body.classList.toggle('sidebar-collapsed', readSidebarPreference());
        }

        relocateSidebarToggle();
        updateSidebarAccessibility();
    };

    function toggleSidebar() {
        if (isMobileViewport()) {
            body.classList.toggle('sidebar-open');
        } else {
            const nextCollapsedState = !body.classList.contains('sidebar-collapsed');
            body.classList.toggle('sidebar-collapsed', nextCollapsedState);
            saveSidebarPreference(nextCollapsedState);
        }

        updateSidebarAccessibility();
    }

    const setActiveSidebarLink = () => {
        const navLinks = document.querySelectorAll('.app-sidebar .sidebar-link[href]');
        const currentPath = window.location.pathname;

        navLinks.forEach((link) => {
            const targetPath = new URL(link.getAttribute('href'), window.location.origin).pathname;
            let isActive = false;

            if (targetPath === '/dashboard') {
                isActive = currentPath === '/' || currentPath === '/dashboard';
            } else {
                isActive = currentPath === targetPath || currentPath.startsWith(`${targetPath}/`);
            }

            link.classList.toggle('active', isActive);
        });
    };

    if (sidebarToggle) {
        sidebarToggle.addEventListener('click', toggleSidebar);
    }

    if (sidebarBackdrop) {
        sidebarBackdrop.addEventListener('click', closeMobileSidebar);
    }

    if (userMenuToggle) {
        userMenuToggle.addEventListener('click', (event) => {
            event.stopPropagation();
            if (userMenu) {
                userMenu.classList.toggle('open');
            }
        });
    }

    document.addEventListener('click', (event) => {
        if (!userMenu || !userMenu.contains(event.target)) {
            closeUserMenu();
        }
    });

    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape') {
            closeUserMenu();
            closeMobileSidebar();
        }
    });

    window.addEventListener('resize', syncSidebarLayout);
    window.addEventListener('online', updateConnectionStatus);
    window.addEventListener('offline', updateConnectionStatus);

    syncSidebarLayout();
    setActiveSidebarLink();
    updateConnectionStatus();
    updateDateTime();
    setInterval(updateDateTime, 1000);

    window.toggleSidebar = toggleSidebar;

    window.setTimeout(() => {
        body.classList.remove('dashboard-loading');
    }, 380);

    if (window.lucide && typeof window.lucide.createIcons === 'function') {
        window.lucide.createIcons();
    }
})();
