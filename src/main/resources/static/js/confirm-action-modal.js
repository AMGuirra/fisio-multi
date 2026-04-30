(() => {
    const modal = document.getElementById('confirmActionModal');
    if (!modal) {
        return;
    }

    const body = document.body;
    const title = document.getElementById('confirmActionModalTitle');
    const message = document.getElementById('confirmActionModalMessage');
    const helperTitle = document.getElementById('confirmActionModalHelperTitle');
    const helperMessage = document.getElementById('confirmActionModalHelperMessage');
    const confirmButton = document.getElementById('confirmActionModalSubmit');
    const confirmLabel = document.getElementById('confirmActionModalSubmitLabel');

    let pendingForm = null;
    let lastActiveElement = null;

    const syncBodyModalState = () => {
        const hasOpenModal = document.querySelector('.app-modal.is-open') !== null;
        body.classList.toggle('modal-open', hasOpenModal);
    };

    const setModalOpen = (open) => {
        modal.classList.toggle('is-open', open);
        modal.setAttribute('aria-hidden', open ? 'false' : 'true');
        syncBodyModalState();
        if (open) {
            confirmButton.focus();
            return;
        }
        if (lastActiveElement && typeof lastActiveElement.focus === 'function') {
            lastActiveElement.focus();
        }
    };

    const closeModal = () => {
        pendingForm = null;
        setModalOpen(false);
    };

    const openForForm = (form, submitter) => {
        pendingForm = form;
        lastActiveElement = submitter || document.activeElement;

        title.textContent = form.dataset.confirmTitle || 'Confirmar ação';
        message.textContent = form.dataset.confirmMessage || 'Deseja continuar com esta ação?';
        helperTitle.textContent = form.dataset.confirmHelperTitle || 'Essa ação exige confirmação';
        helperMessage.textContent = form.dataset.confirmHelperMessage || 'Os dados serão enviados assim que você confirmar.';
        confirmLabel.textContent = form.dataset.confirmActionLabel || 'Confirmar';

        setModalOpen(true);
    };

    document.querySelectorAll('form[data-confirm-modal]').forEach((form) => {
        form.addEventListener('submit', (event) => {
            event.preventDefault();
            openForForm(form, event.submitter || form.querySelector('[type="submit"]'));
        });
    });

    modal.querySelectorAll('[data-modal-close]').forEach((element) => {
        element.addEventListener('click', closeModal);
    });

    confirmButton.addEventListener('click', () => {
        if (!pendingForm) {
            return;
        }
        const formToSubmit = pendingForm;
        pendingForm = null;
        setModalOpen(false);
        HTMLFormElement.prototype.submit.call(formToSubmit);
    });

    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape' && modal.classList.contains('is-open')) {
            closeModal();
        }
    });
})();
