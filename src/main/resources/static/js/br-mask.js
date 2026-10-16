/*
 * Máscaras BR tolerantes a preenchimento automático do navegador.
 * O autofill pode entregar "+5567981101560", "5567981101560", "(67) 98110-1560",
 * "067 98110-1560" etc.; tudo é normalizado para dígitos nacionais antes de mascarar.
 */
(function (global) {
    const digits = (v) => (v || '').replace(/\D/g, '');

    const telefoneNacional = (value) => {
        let d = digits(value);
        // Código do país (55): só descartamos quando sobram DDD + número (10 ou 11 dígitos).
        if (d.startsWith('55') && d.length >= 12) {
            d = d.slice(2);
        }
        // Zero de operadora/tronco antes do DDD (ex.: 067...).
        if (d.startsWith('0') && d.length >= 11) {
            d = d.replace(/^0+/, '');
        }
        return d.slice(0, 11);
    };

    const telefone = (value) => {
        const d = telefoneNacional(value);
        if (d.length <= 10) {
            return d.replace(/^(\d{2})(\d)/, '($1) $2').replace(/(\d{4})(\d)/, '$1-$2');
        }
        return d.replace(/^(\d{2})(\d)/, '($1) $2').replace(/(\d{5})(\d)/, '$1-$2');
    };

    const cpf = (value) => digits(value).slice(0, 11)
        .replace(/^(\d{3})(\d)/, '$1.$2')
        .replace(/^(\d{3})\.(\d{3})(\d)/, '$1.$2.$3')
        .replace(/\.(\d{3})(\d)/, '.$1-$2');

    const cep = (value) => digits(value).slice(0, 8).replace(/^(\d{5})(\d)/, '$1-$2');

    // Reaplica a máscara em digitação, autofill (input/change), blur e no valor já presente ao carregar.
    const bind = (input, fn) => {
        if (!input) {
            return;
        }
        const apply = () => {
            const masked = fn(input.value);
            if (masked !== input.value) {
                input.value = masked;
            }
        };
        ['input', 'change', 'blur', 'paste'].forEach((ev) =>
            input.addEventListener(ev, () => (ev === 'paste' ? setTimeout(apply, 0) : apply())));
        // Chrome pode preencher sem disparar eventos até a primeira interação.
        input.addEventListener('animationstart', apply);
        [100, 500, 1500].forEach((ms) => setTimeout(apply, ms));
        apply();
    };

    global.BrMask = { digits, telefoneNacional, telefone, cpf, cep, bind };
})(window);
