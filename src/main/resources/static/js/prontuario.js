(() => {
    'use strict';

    const MAX_FOTOS = 6;
    const TIPOS_PERMITIDOS = ['image/jpeg', 'image/png', 'image/webp'];

    // ---------------------------------------------------------------
    // Textareas: crescimento automático + contador de caracteres
    // ---------------------------------------------------------------

    const autoGrow = (textarea) => {
        textarea.style.height = 'auto';
        textarea.style.height = textarea.scrollHeight + 'px';
    };

    document.querySelectorAll('textarea[data-autogrow]').forEach((textarea) => {
        const maxLength = parseInt(textarea.getAttribute('maxlength'), 10) || null;
        const counterId = textarea.getAttribute('data-char-counter');
        const counter = counterId ? document.getElementById(counterId) : null;

        const updateCounter = () => {
            if (!counter) {
                return;
            }
            const length = textarea.value.length;
            counter.textContent = maxLength ? `${length} / ${maxLength} caracteres` : `${length} caracteres`;
        };

        autoGrow(textarea);
        updateCounter();

        textarea.addEventListener('input', () => {
            autoGrow(textarea);
            updateCounter();
        });
    });

    // ---------------------------------------------------------------
    // Avaliação: cancelar restaura o último valor persistido
    // ---------------------------------------------------------------

    const btnCancelarAvaliacao = document.getElementById('btnCancelarAvaliacao');
    if (btnCancelarAvaliacao) {
        btnCancelarAvaliacao.addEventListener('click', () => {
            const targetId = btnCancelarAvaliacao.getAttribute('data-cancel-target');
            const textarea = targetId ? document.getElementById(targetId) : null;
            if (!textarea) {
                return;
            }
            const original = textarea.getAttribute('data-original') || '';
            if (textarea.value !== original && textarea.value.trim() !== '') {
                const confirmado = window.confirm('Descartar as alterações não salvas da avaliação?');
                if (!confirmado) {
                    return;
                }
            }
            textarea.value = original;
            autoGrow(textarea);
            textarea.dispatchEvent(new Event('input'));
        });
    }

    // ---------------------------------------------------------------
    // Evolução: seleção de fotos, preview, remoção e consentimento
    // ---------------------------------------------------------------

    const inputFotos = document.getElementById('evolucaoFotos');
    const previewContainer = document.getElementById('fotoPreviews');
    const fotoErro = document.getElementById('fotoErro');
    const consentimentoWrapper = document.getElementById('consentimentoWrapper');
    const consentimentoCheckbox = document.getElementById('consentimentoFotos');

    /** Lista de arquivos atualmente selecionados (mantida em memória, sincronizada com o input via DataTransfer). */
    let arquivosSelecionados = [];
    let previewUrls = [];

    const limparPreviewUrls = () => {
        previewUrls.forEach((url) => URL.revokeObjectURL(url));
        previewUrls = [];
    };

    const mostrarErroFoto = (mensagem) => {
        if (!fotoErro) {
            return;
        }
        if (!mensagem) {
            fotoErro.hidden = true;
            fotoErro.textContent = '';
            return;
        }
        fotoErro.hidden = false;
        fotoErro.textContent = mensagem;
    };

    const sincronizarInputFotos = () => {
        if (!inputFotos || typeof DataTransfer === 'undefined') {
            return;
        }
        const dataTransfer = new DataTransfer();
        arquivosSelecionados.forEach((arquivo) => dataTransfer.items.add(arquivo));
        inputFotos.files = dataTransfer.files;
    };

    const atualizarConsentimento = () => {
        if (!consentimentoWrapper) {
            return;
        }
        const temFotos = arquivosSelecionados.length > 0;
        consentimentoWrapper.hidden = !temFotos;
        if (!temFotos && consentimentoCheckbox) {
            consentimentoCheckbox.checked = false;
        }
    };

    const renderPreviews = () => {
        if (!previewContainer) {
            return;
        }
        limparPreviewUrls();
        previewContainer.innerHTML = '';

        arquivosSelecionados.forEach((arquivo, index) => {
            const url = URL.createObjectURL(arquivo);
            previewUrls.push(url);

            const wrapper = document.createElement('div');
            wrapper.className = 'prontuario-photo-preview';

            const img = document.createElement('img');
            img.src = url;
            img.alt = 'Pré-visualização da foto selecionada';
            wrapper.appendChild(img);

            const removerBtn = document.createElement('button');
            removerBtn.type = 'button';
            removerBtn.setAttribute('aria-label', 'Remover esta foto da seleção');
            removerBtn.innerHTML = '<i data-lucide="x"></i>';
            removerBtn.addEventListener('click', () => {
                arquivosSelecionados.splice(index, 1);
                sincronizarInputFotos();
                renderPreviews();
                atualizarConsentimento();
            });
            wrapper.appendChild(removerBtn);

            previewContainer.appendChild(wrapper);
        });

        if (window.lucide && typeof window.lucide.createIcons === 'function') {
            window.lucide.createIcons();
        }
    };

    if (inputFotos) {
        inputFotos.addEventListener('change', () => {
            mostrarErroFoto(null);
            const novos = Array.from(inputFotos.files || []);

            const invalidos = novos.filter((arquivo) => !TIPOS_PERMITIDOS.includes(arquivo.type));
            if (invalidos.length > 0) {
                mostrarErroFoto('Envie apenas fotos em JPEG, PNG ou WebP.');
            }

            const validos = novos.filter((arquivo) => TIPOS_PERMITIDOS.includes(arquivo.type));
            const combinados = [...arquivosSelecionados, ...validos];

            if (combinados.length > MAX_FOTOS) {
                mostrarErroFoto(`Você pode anexar no máximo ${MAX_FOTOS} fotos por evolução.`);
                arquivosSelecionados = combinados.slice(0, MAX_FOTOS);
            } else {
                arquivosSelecionados = combinados;
            }

            sincronizarInputFotos();
            renderPreviews();
            atualizarConsentimento();
        });
    }

    // ---------------------------------------------------------------
    // Limpar evolução (não afeta registros já salvos)
    // ---------------------------------------------------------------

    const btnLimparEvolucao = document.getElementById('btnLimparEvolucao');
    if (btnLimparEvolucao) {
        btnLimparEvolucao.addEventListener('click', () => {
            const form = document.getElementById('formEvolucao');
            if (!form) {
                return;
            }
            const texto = document.getElementById('evolucaoTexto');
            const temConteudo = (texto && texto.value.trim() !== '') || arquivosSelecionados.length > 0;

            if (temConteudo) {
                const confirmado = window.confirm('Limpar o formulário de evolução? O texto e as fotos selecionadas serão descartados.');
                if (!confirmado) {
                    return;
                }
            }

            if (texto) {
                texto.value = '';
                autoGrow(texto);
                texto.dispatchEvent(new Event('input'));
            }
            const regiao = document.getElementById('regiaoFotos');
            const observacao = document.getElementById('observacaoFotos');
            if (regiao) regiao.value = '';
            if (observacao) observacao.value = '';

            arquivosSelecionados = [];
            sincronizarInputFotos();
            renderPreviews();
            atualizarConsentimento();
            mostrarErroFoto(null);
        });
    }

    // ---------------------------------------------------------------
    // Histórico: alterna cada evolução entre leitura e edição inline
    // ---------------------------------------------------------------

    const alternarEdicaoEvolucao = (targetId, editando) => {
        const entry = document.getElementById(targetId);
        if (!entry) {
            return;
        }
        const leitura = entry.querySelector('[data-role="entry-read"]');
        const edicao = entry.querySelector('[data-role="entry-edit"]');
        if (!leitura || !edicao) {
            return;
        }
        leitura.hidden = editando;
        edicao.hidden = !editando;
        if (editando) {
            const textarea = edicao.querySelector('textarea');
            if (textarea) {
                autoGrow(textarea);
                textarea.focus();
            }
        }
    };

    document.querySelectorAll('.prontuario-entry-edit-toggle').forEach((botao) => {
        botao.addEventListener('click', () => {
            alternarEdicaoEvolucao(botao.getAttribute('data-target'), true);
        });
    });

    document.querySelectorAll('.prontuario-entry-edit-cancel').forEach((botao) => {
        botao.addEventListener('click', () => {
            alternarEdicaoEvolucao(botao.getAttribute('data-target'), false);
        });
    });

    // ---------------------------------------------------------------
    // Impede duplo envio nos formulários do prontuário
    // ---------------------------------------------------------------

    document.querySelectorAll('form[data-submit-guard]').forEach((form) => {
        form.addEventListener('submit', (event) => {
            const submitButton = form.querySelector('button[type="submit"]');
            if (!submitButton) {
                return;
            }
            if (submitButton.dataset.submitted === 'true') {
                event.preventDefault();
                return;
            }
            submitButton.dataset.submitted = 'true';
            submitButton.disabled = true;
            const label = submitButton.querySelector('span');
            const loadingLabel = submitButton.getAttribute('data-loading-label');
            if (label && loadingLabel) {
                label.textContent = loadingLabel;
            }
        });
    });

    // ---------------------------------------------------------------
    // Lightbox de fotos clínicas do histórico
    // ---------------------------------------------------------------

    const lightbox = document.getElementById('prontuarioLightbox');
    if (lightbox) {
        const image = document.getElementById('prontuarioLightboxImage');
        const regiaoEl = document.getElementById('prontuarioLightboxTitle');
        const observacaoEl = document.getElementById('prontuarioLightboxObservacao');
        const infoEl = document.getElementById('prontuarioLightboxInfo');
        const downloadEl = document.getElementById('prontuarioLightboxDownload');
        const btnPrev = lightbox.querySelector('[data-lightbox-prev]');
        const btnNext = lightbox.querySelector('[data-lightbox-next]');

        let grupoAtual = [];
        let indiceAtual = 0;
        let elementoAnterior = null;

        const renderFoto = () => {
            const botao = grupoAtual[indiceAtual];
            if (!botao) {
                return;
            }
            image.src = botao.getAttribute('data-lightbox-src');
            image.alt = 'Foto clínica ampliada';
            regiaoEl.textContent = botao.getAttribute('data-lightbox-regiao') || 'Região não informada';
            observacaoEl.textContent = botao.getAttribute('data-lightbox-observacao') || '';
            const dataFoto = botao.getAttribute('data-lightbox-data') || '';
            const autor = botao.getAttribute('data-lightbox-autor') || '';
            infoEl.textContent = [dataFoto, autor].filter(Boolean).join(' · ');
            downloadEl.setAttribute('href', botao.getAttribute('data-lightbox-download') || '#');

            const temVarias = grupoAtual.length > 1;
            btnPrev.hidden = !temVarias;
            btnNext.hidden = !temVarias;
        };

        const abrirLightbox = (botaoClicado) => {
            const container = botaoClicado.closest('.prontuario-photo-thumbs');
            grupoAtual = container ? Array.from(container.querySelectorAll('[data-lightbox-group]')) : [botaoClicado];
            indiceAtual = grupoAtual.indexOf(botaoClicado);
            elementoAnterior = botaoClicado;

            renderFoto();
            lightbox.classList.add('is-open');
            lightbox.setAttribute('aria-hidden', 'false');
            document.body.classList.add('modal-open');
            lightbox.querySelector('.prontuario-lightbox-close').focus();
        };

        const fecharLightbox = () => {
            lightbox.classList.remove('is-open');
            lightbox.setAttribute('aria-hidden', 'true');
            document.body.classList.remove('modal-open');
            image.src = '';
            if (elementoAnterior && typeof elementoAnterior.focus === 'function') {
                elementoAnterior.focus();
            }
        };

        document.querySelectorAll('[data-lightbox-group]').forEach((botao) => {
            botao.addEventListener('click', () => abrirLightbox(botao));
        });

        lightbox.querySelectorAll('[data-lightbox-close]').forEach((el) => {
            el.addEventListener('click', fecharLightbox);
        });

        btnPrev.addEventListener('click', () => {
            indiceAtual = (indiceAtual - 1 + grupoAtual.length) % grupoAtual.length;
            renderFoto();
        });

        btnNext.addEventListener('click', () => {
            indiceAtual = (indiceAtual + 1) % grupoAtual.length;
            renderFoto();
        });

        document.addEventListener('keydown', (event) => {
            if (!lightbox.classList.contains('is-open')) {
                return;
            }
            if (event.key === 'Escape') {
                fecharLightbox();
            } else if (event.key === 'ArrowLeft') {
                btnPrev.click();
            } else if (event.key === 'ArrowRight') {
                btnNext.click();
            }
        });
    }
})();
