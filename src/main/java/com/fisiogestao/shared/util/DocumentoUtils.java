package com.fisiogestao.shared.util;

/**
 * Normalização e validação centralizada de CPF e celular/telefone.
 *
 * <p>Único ponto de verdade para essas regras no sistema: controllers, services e forms
 * devem delegar para cá em vez de reimplementar regex/checksum próprios, evitando que a
 * mesma lógica (e os mesmos bugs) se espalhem por múltiplas classes.</p>
 */
public final class DocumentoUtils {

    private DocumentoUtils() {
    }

    /**
     * Remove todo caractere não numérico. Aceita entrada com ou sem máscara, com espaços,
     * pontos, traços, barras ou parênteses. Retorna {@code null} para entrada nula/vazia.
     */
    public static String somenteDigitos(String valor) {
        if (valor == null) {
            return null;
        }
        String digits = valor.replaceAll("\\D", "");
        return digits.isBlank() ? null : digits;
    }

    /** Normaliza um CPF para os 11 dígitos, sem validar dígitos verificadores. */
    public static String normalizarCpf(String valor) {
        return somenteDigitos(valor);
    }

    /** Normaliza um celular/telefone para dígitos, sem validar quantidade. */
    public static String normalizarTelefone(String valor) {
        return somenteDigitos(valor);
    }

    /** {@code true} se o CPF normalizado tem exatamente 11 dígitos. */
    public static boolean cpfTemTamanhoValido(String cpfNormalizado) {
        return cpfNormalizado != null && cpfNormalizado.length() == 11;
    }

    /**
     * Valida um CPF já normalizado (11 dígitos): rejeita sequências com todos os dígitos
     * iguais (ex.: {@code 111.111.111-11}) e confere os dois dígitos verificadores.
     */
    public static boolean cpfValido(String cpfNormalizado) {
        if (!cpfTemTamanhoValido(cpfNormalizado)) {
            return false;
        }
        if (cpfNormalizado.chars().distinct().count() == 1) {
            return false;
        }
        int[] digitos = cpfNormalizado.chars().map(c -> c - '0').toArray();
        return digitos[9] == calcularDigitoVerificadorCpf(digitos, 9)
                && digitos[10] == calcularDigitoVerificadorCpf(digitos, 10);
    }

    private static int calcularDigitoVerificadorCpf(int[] digitos, int quantidade) {
        int soma = 0;
        int peso = quantidade + 1;
        for (int i = 0; i < quantidade; i++) {
            soma += digitos[i] * peso;
            peso--;
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    /**
     * {@code true} se o celular normalizado tem 10 (fixo/celular com DDD) ou 11 dígitos
     * (celular com DDD e o nono dígito), que são os formatos já aceitos hoje pelo sistema.
     */
    public static boolean telefoneTemTamanhoValido(String telefoneNormalizado) {
        return telefoneNormalizado != null
                && (telefoneNormalizado.length() == 10 || telefoneNormalizado.length() == 11);
    }
}
