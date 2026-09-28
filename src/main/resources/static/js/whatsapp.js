/**
 * Envio de mensagens por WhatsApp sem API (links wa.me).
 * Abre o WhatsApp Web/Desktop ou o app do celular com o destinatário e o texto preenchidos.
 *
 * Uso programático: window.abrirWhatsAppSemApi('(47) 99999-8888', 'Olá!');
 * Uso declarativo (compatível com CSP, sem onclick inline):
 *   <button type="button" data-whatsapp-telefone="47999998888" data-whatsapp-mensagem="Olá!">WhatsApp</button>
 */
(function () {
    'use strict';

    const DDI_BRASIL = '55';

    // Mesma regra do WhatsAppUrlBuilder (Java): 10/11 dígitos recebem DDI 55; "0 + DDD" e
    // "0 + operadora + DDD" também; 12 a 15 dígitos sem o 0 inicial já têm DDI.
    function normalizarNumero(telefone) {
        if (!telefone) {
            return null;
        }
        const digitos = String(telefone).replace(/\D/g, '');
        if (digitos.startsWith('0')) {
            if (digitos.startsWith('00')) {
                return null;
            }
            let semZero = digitos.slice(1);
            if (semZero.length === 12 || semZero.length === 13) {
                semZero = semZero.slice(2);
            }
            return semZero.length === 10 || semZero.length === 11 ? DDI_BRASIL + semZero : null;
        }
        if (digitos.length === 10 || digitos.length === 11) {
            return DDI_BRASIL + digitos;
        }
        if (digitos.length >= 12 && digitos.length <= 15) {
            return digitos;
        }
        return null;
    }

    function avisar(mensagem) {
        if (typeof window.showToast === 'function') {
            window.showToast(mensagem, 'error');
        } else {
            window.alert(mensagem);
        }
    }

    function abrirWhatsAppSemApi(telefone, mensagem) {
        const numero = normalizarNumero(telefone);
        if (!numero) {
            avisar(telefone ? 'Telefone do paciente inválido para WhatsApp.' : 'Paciente não possui telefone cadastrado.');
            return;
        }
        const texto = mensagem ? '?text=' + encodeURIComponent(mensagem) : '';
        window.open('https://wa.me/' + numero + texto, '_blank', 'noopener');
    }

    document.addEventListener('click', function (event) {
        const gatilho = event.target.closest('[data-whatsapp-telefone]');
        if (!gatilho) {
            return;
        }
        event.preventDefault();
        abrirWhatsAppSemApi(gatilho.dataset.whatsappTelefone, gatilho.dataset.whatsappMensagem || '');
    });

    window.abrirWhatsAppSemApi = abrirWhatsAppSemApi;
})();
