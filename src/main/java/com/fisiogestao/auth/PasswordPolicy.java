package com.fisiogestao.auth;

import com.fisiogestao.shared.web.BusinessException;

public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 120;
    public static final String REQUIRED_MESSAGE = "Informe a senha.";
    public static final String MIN_LENGTH_MESSAGE = "A senha deve ter pelo menos 8 caracteres.";
    public static final String RANGE_MESSAGE = "A senha deve ter entre 8 e 120 caracteres.";

    private PasswordPolicy() {
    }

    public static String normalize(String password, boolean required) {
        if (password == null) {
            if (required) {
                throw new BusinessException(REQUIRED_MESSAGE);
            }
            return null;
        }

        String normalized = password.trim();
        if (normalized.isBlank()) {
            if (required) {
                throw new BusinessException(REQUIRED_MESSAGE);
            }
            return null;
        }
        if (normalized.length() < MIN_LENGTH) {
            throw new BusinessException(MIN_LENGTH_MESSAGE);
        }
        if (normalized.length() > MAX_LENGTH) {
            throw new BusinessException(RANGE_MESSAGE);
        }
        return normalized;
    }
}
