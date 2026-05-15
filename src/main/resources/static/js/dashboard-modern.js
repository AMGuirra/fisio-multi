(() => {
    const body = document.body;
    const finishDashboardLoading = () => {
        if (body) {
            body.classList.remove('dashboard-loading');
        }
    };

    window.setTimeout(finishDashboardLoading, 380);

    try {
        const appSidebar = document.getElementById('appSidebar');
        const sidebarToggle = document.getElementById('sidebarToggle');
        const sidebarBackdrop = document.getElementById('sidebarBackdrop');
        const appHeader = document.querySelector('.app-header');
        const headerCopy = appHeader ? appHeader.querySelector('.header-copy') : null;
        const headerRight = appHeader ? appHeader.querySelector('.header-right') : null;
        const userMenu = document.getElementById('userMenu');
        const userMenuToggle = document.getElementById('userMenuToggle');
        const greetingElement = document.getElementById('dynamicGreeting');
        let headerDateElement = document.getElementById('headerFullDate');
        let infoDateElement = document.getElementById('infoDate');
        let liveClockElement = document.getElementById('liveClock');
        let infoTimeElement = document.getElementById('infoTime');
        let systemStatusElement = document.getElementById('systemStatus');

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

        const ensureSharedHeader = () => {
            if (!appHeader || !headerRight) {
                return;
            }

            appHeader.classList.add('dashboard-header');

            if (headerDateElement && headerCopy && headerDateElement.classList.contains('header-date')) {
                headerDateElement.className = 'header-context';
                headerDateElement.textContent = 'Painel clínico em tempo real para a sua rotina.';
            }

            if (!headerRight.querySelector('.header-insights')) {
                const insights = document.createElement('div');
                insights.className = 'header-insights';
                insights.setAttribute('aria-label', 'Informações do sistema');
                insights.innerHTML = `
                    <div class="header-info-chip">
                        <span class="header-info-icon status"><i data-lucide="activity" aria-hidden="true"></i></span>
                        <span class="header-info-copy">
                            <span class="header-info-label">Status do sistema</span>
                            <span class="header-info-value skeletonable system-status" id="systemStatus">Online</span>
                        </span>
                    </div>
                    <div class="header-info-chip">
                        <span class="header-info-icon calendar"><i data-lucide="calendar-days" aria-hidden="true"></i></span>
                        <span class="header-info-copy">
                            <span class="header-info-label">Data atual</span>
                            <span class="header-info-value skeletonable" id="infoDate">00/00/0000</span>
                        </span>
                    </div>
                    <div class="header-info-chip">
                        <span class="header-info-icon clock"><i data-lucide="timer-reset" aria-hidden="true"></i></span>
                        <span class="header-info-copy">
                            <span class="header-info-label">Hora atual</span>
                            <span class="header-info-value skeletonable" id="infoTime">00:00:00</span>
                        </span>
                    </div>
                `;
                headerRight.insertBefore(insights, headerRight.firstChild);

                infoDateElement = insights.querySelector('#infoDate');
                infoTimeElement = insights.querySelector('#infoTime');
                systemStatusElement = insights.querySelector('#systemStatus');
            }

            if (liveClockElement) {
                liveClockElement.style.display = 'none';
                liveClockElement.setAttribute('aria-hidden', 'true');
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

            if (headerDateElement && headerDateElement.classList.contains('header-date')) {
                headerDateElement.textContent = fullDate;
            }

            if (infoDateElement) {
                infoDateElement.textContent = shortDate;
            }

            if (liveClockElement && liveClockElement.style.display !== 'none') {
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

        const formatCurrency = (value) => new Intl.NumberFormat('pt-BR', {
            style: 'currency',
            currency: 'BRL',
            maximumFractionDigits: 0
        }).format(Number(value || 0));

        const monthLabel = (value) => {
            const [year, month] = String(value || '').split('-').map(Number);
            if (!year || !month) {
                return '--';
            }
            return new Date(year, month - 1, 1).toLocaleDateString('pt-BR', {
                month: 'short'
            }).replace('.', '');
        };

        const setText = (id, value) => {
            const element = document.getElementById(id);
            if (element) {
                element.textContent = value;
            }
        };

        const renderBars = (container, items, options) => {
            if (!container) {
                return;
            }

            if (!items || items.length === 0) {
                container.innerHTML = '<p class="analytics-empty">Sem dados no periodo.</p>';
                return;
            }

            const max = Math.max(...items.map(options.value), 1);
            container.innerHTML = items.map((item) => {
                const value = options.value(item);
                const percent = Math.max(4, Math.round((value / max) * 100));
                const label = options.labelFn ? options.labelFn(item) : monthLabel(item.mes);
                return `
                    <div class="chart-row">
                        <span class="chart-label" title="${label}">${label}</span>
                        <span class="chart-track" title="${options.title(item)}">
                            <span class="chart-fill ${options.fillClass || ''}" style="width:${percent}%"></span>
                        </span>
                        <span class="chart-value">${options.display(item)}</span>
                    </div>
                `;
            }).join('');
        };

        const carregarMetricasDashboard = async () => {
            const ocupacaoChart = document.getElementById('ocupacaoChart');
            const receitaChart = document.getElementById('receitaChart');
            const frequenciaChart = document.getElementById('frequenciaChart');
            if (!ocupacaoChart && !receitaChart && !frequenciaChart) {
                return;
            }

            try {
                const response = await fetch('/api/dashboard/metricas', {
                    headers: {
                        Accept: 'application/json'
                    },
                    credentials: 'same-origin'
                });

                if (!response.ok) {
                    throw new Error(`HTTP ${response.status}`);
                }

                const data = await response.json();
                const ocupacao = data.ocupacaoMensal || [];
                const receitas = data.receitaMensal || [];
                const retencao = data.retencaoPlanos || {};
                const frequencia = data.rankingFrequencia || [];

                const ocupacaoMedia = ocupacao.length
                    ? ocupacao.reduce((total, item) => total + Number(item.taxaOcupacao || 0), 0) / ocupacao.length
                    : 0;
                const receitaTotal = receitas.reduce((total, item) => total + Number(item.totalReceita || 0), 0);

                setText('ocupacaoMedia', `${ocupacaoMedia.toFixed(1)}%`);
                setText('receitaTotal', formatCurrency(receitaTotal));
                setText('taxaConversao', `${Number(retencao.taxaConversao || 0).toFixed(1)}%`);
                setText('taxaRenovacao', `${Number(retencao.taxaRenovacao || 0).toFixed(1)}%`);
                setText(
                    'retencaoBase',
                    `${retencao.pacientesComPlano || 0} de ${retencao.totalPacientesAtivos || 0} pacientes ativos com plano`
                );

                renderBars(ocupacaoChart, ocupacao, {
                    value: (item) => Number(item.taxaOcupacao || 0),
                    display: (item) => `${Number(item.taxaOcupacao || 0).toFixed(1)}%`,
                    title: (item) => `${item.totalSessoes || 0} sessoes, ${Number(item.horasAgendadas || 0).toFixed(1)}h de ${Number(item.capacidadeHoras || 0).toFixed(1)}h`,
                    fillClass: 'chart-fill-ocupacao'
                });

                renderBars(receitaChart, receitas, {
                    value: (item) => Number(item.totalReceita || 0),
                    display: (item) => formatCurrency(item.totalReceita),
                    title: (item) => `Planos ${formatCurrency(item.receitaPlano)} / Avulso ${formatCurrency(item.receitaAvulso)}`,
                    fillClass: 'chart-fill-receita'
                });

                renderBars(frequenciaChart, frequencia, {
                    value: (item) => Number(item.totalPresencas || 0),
                    display: (item) => `${item.totalPresencas} sessões`,
                    title: (item) => `${item.totalPresencas} sessões realizadas de ${item.totalSessoes} sessões totais`,
                    labelFn: (item) => item.nomePaciente,
                    fillClass: 'chart-fill-frequencia'
                });

                if (frequencia.length === 0 && frequenciaChart) {
                    frequenciaChart.innerHTML = '<p class="analytics-empty">Nenhuma atividade registrada no período.</p>';
                }
            } catch (error) {
                if (ocupacaoChart) {
                    ocupacaoChart.innerHTML = '<p class="analytics-empty">Nao foi possivel carregar os indicadores.</p>';
                }
                if (receitaChart) {
                    receitaChart.innerHTML = '<p class="analytics-empty">Nao foi possivel carregar os indicadores.</p>';
                }
                if (frequenciaChart) {
                    frequenciaChart.innerHTML = '<p class="analytics-empty">Nao foi possivel carregar os indicadores.</p>';
                }
                console.error('Falha ao carregar metricas do dashboard.', error);
            }
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

            const sidebarHeader = appSidebar.querySelector('.sidebar-header');
            if (sidebarHeader && sidebarToggle.parentNode !== sidebarHeader) {
                sidebarHeader.appendChild(sidebarToggle);
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

        ensureSharedHeader();
        syncSidebarLayout();
        setActiveSidebarLink();
        updateConnectionStatus();
        updateDateTime();
        carregarMetricasDashboard();
        setInterval(updateDateTime, 1000);

        window.toggleSidebar = toggleSidebar;

        if (window.lucide && typeof window.lucide.createIcons === 'function') {
            window.lucide.createIcons();
        }
    } catch (error) {
        console.error('Falha ao inicializar a interface do painel.', error);
        finishDashboardLoading();
    }
})();
