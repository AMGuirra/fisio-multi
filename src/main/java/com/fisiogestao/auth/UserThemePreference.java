package com.fisiogestao.auth;

import java.util.Locale;

public enum UserThemePreference {
    LIGHT("light", "Claro"),
    DARK("dark", "Escuro");

    private final String cssValue;
    private final String label;

    UserThemePreference(String cssValue, String label) {
        this.cssValue = cssValue;
        this.label = label;
    }

    public String getCssValue() {
        return cssValue;
    }

    public String getLabel() {
        return label;
    }

    public static UserThemePreference fallback(UserThemePreference theme) {
        return theme != null ? theme : LIGHT;
    }

    public static String toCssValue(UserThemePreference theme) {
        return fallback(theme).name().toLowerCase(Locale.ROOT);
    }
}
