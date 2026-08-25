(() => {
    const tabForHash = {
        '#avaliacao': 'avaliacao',
        '#novaEvolucao': 'evolucoes',
        '#historicoEvolucao': 'evolucoes',
        '#documentosAnexos': 'documentos'
    };

    const activate = (name) => {
        document.querySelectorAll('[data-prontuario-tab]').forEach((tab) => {
            const active = tab.dataset.prontuarioTab === name;
            tab.classList.toggle('active', active);
            tab.setAttribute('aria-selected', String(active));
        });
        document.querySelectorAll('[data-prontuario-panel]').forEach((panel) => {
            panel.hidden = panel.dataset.prontuarioPanel !== name;
        });
    };

    document.addEventListener('DOMContentLoaded', () => {
        activate(tabForHash[window.location.hash] || 'visao-geral');
        document.querySelectorAll('[data-prontuario-tab]').forEach((tab) =>
            tab.addEventListener('click', () => activate(tab.dataset.prontuarioTab)));

        document.querySelectorAll('[data-historico-filter]').forEach((filter) => {
            filter.addEventListener('click', () => {
                const type = filter.dataset.historicoFilter;
                document.querySelectorAll('[data-historico-filter]').forEach((chip) => chip.classList.toggle('active', chip === filter));
                document.querySelectorAll('.timeline-item[data-tipo]').forEach((item) => {
                    item.hidden = type !== 'todos' && item.dataset.tipo !== type
                        && !(type === 'documento' && item.dataset.tipo === 'foto');
                });
            });
        });
    });
})();
