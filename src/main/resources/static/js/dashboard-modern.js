(() => {
    const body = document.body;
    const sidebarCollapseToggle = document.getElementById('sidebarCollapseToggle');
    const mobileMenuToggle = document.getElementById('mobileMenuToggle');
    const sidebarBackdrop = document.getElementById('sidebarBackdrop');
    const userMenu = document.getElementById('userMenu');
    const userMenuToggle = document.getElementById('userMenuToggle');
    const greetingElement = document.getElementById('dynamicGreeting');
    const headerDateElement = document.getElementById('headerFullDate');
    const infoDateElement = document.getElementById('infoDate');
    const liveClockElement = document.getElementById('liveClock');
    const infoTimeElement = document.getElementById('infoTime');
    const systemStatusElement = document.getElementById('systemStatus');

    const storageKey = 'fisiogestao.sidebar.collapsed';

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

    const toggleSidebarCollapsed = () => {
        const nextState = !body.classList.contains('sidebar-collapsed');
        body.classList.toggle('sidebar-collapsed', nextState);
        localStorage.setItem(storageKey, String(nextState));
    };

    const toggleMobileSidebar = () => {
        body.classList.toggle('sidebar-open');
    };

    const closeMobileSidebar = () => {
        body.classList.remove('sidebar-open');
    };

    const restoreSidebarState = () => {
        const saved = localStorage.getItem(storageKey);
        if (saved === 'true' && window.innerWidth > 920) {
            body.classList.add('sidebar-collapsed');
        }
    };

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

    if (sidebarCollapseToggle) {
        sidebarCollapseToggle.addEventListener('click', toggleSidebarCollapsed);
    }

    if (mobileMenuToggle) {
        mobileMenuToggle.addEventListener('click', toggleMobileSidebar);
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

    window.addEventListener('resize', () => {
        if (window.innerWidth > 920) {
            closeMobileSidebar();
        }
    });

    window.addEventListener('online', updateConnectionStatus);
    window.addEventListener('offline', updateConnectionStatus);

    restoreSidebarState();
    setActiveSidebarLink();
    updateConnectionStatus();
    updateDateTime();
    setInterval(updateDateTime, 1000);

    window.setTimeout(() => {
        body.classList.remove('dashboard-loading');
    }, 380);

    if (window.lucide && typeof window.lucide.createIcons === 'function') {
        window.lucide.createIcons();
    }
})();
