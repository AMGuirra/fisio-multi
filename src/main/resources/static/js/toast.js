/* ═══════════════════════════════════════════════════════════════════
   FisioGestao — toast.js
   Sistema global de notificações flutuantes.
   Uso: window.showToast('Mensagem aqui', 'success' | 'error' | 'warning' | 'info')
   ═══════════════════════════════════════════════════════════════════ */
(() => {
    const DURATION = 4000;   // ms antes de auto-dismiss
    const ANIM_OUT = 320;    // ms da animação de saída

    const ICONS = {
        success: 'circle-check',
        error:   'x-circle',
        warning: 'triangle-alert',
        info:    'info',
    };

    const LABELS = {
        success: 'Sucesso',
        error:   'Erro',
        warning: 'Atenção',
        info:    'Informação',
    };

    /** Retorna (ou cria) o container fixo no DOM. */
    function getPortal() {
        let el = document.getElementById('toast-portal');
        if (!el) {
            el = document.createElement('div');
            el.id = 'toast-portal';
            el.setAttribute('aria-live', 'polite');
            el.setAttribute('aria-atomic', 'false');
            document.body.appendChild(el);
        }
        return el;
    }

    /**
     * Exibe um toast flutuante.
     * @param {string} message  - Texto da notificação.
     * @param {'success'|'error'|'warning'|'info'} type - Tipo visual.
     */
    function showToast(message, type = 'success') {
        const safeType = ICONS[type] ? type : 'info';
        const portal   = getPortal();

        const toast = document.createElement('div');
        toast.className = `toast toast--${safeType}`;
        toast.setAttribute('role', 'status');

        toast.innerHTML = `
            <span class="toast__icon">
                <i data-lucide="${ICONS[safeType]}" aria-hidden="true"></i>
            </span>
            <span class="toast__body">
                <span class="toast__label">${LABELS[safeType]}</span>
                <span class="toast__msg">${message}</span>
            </span>
            <button class="toast__close" aria-label="Fechar notificação" type="button">
                <i data-lucide="x" aria-hidden="true"></i>
            </button>
        `;

        portal.appendChild(toast);

        // Renderiza ícones Lucide no novo elemento
        if (window.lucide) {
            lucide.createIcons({ nodes: [toast] });
        }

        // Força reflow para disparar a transição CSS
        void toast.offsetWidth;
        toast.classList.add('toast--visible');

        const dismiss = () => {
            if (!toast.isConnected) return;
            // Adiciona --out SEM remover --visible: mesma especificidade no CSS,
            // mas --out vem depois → vence. Garante que a transição de saída
            // parte do estado visível (opacity:1) em vez de ser batched para opacity:0.
            toast.classList.add('toast--out');
            setTimeout(() => {
                if (toast.isConnected) toast.remove();
            }, ANIM_OUT);
        };

        const timer = setTimeout(dismiss, DURATION);

        toast.querySelector('.toast__close').addEventListener('click', () => {
            clearTimeout(timer);
            dismiss();
        });
    }

    window.showToast = showToast;
})();
