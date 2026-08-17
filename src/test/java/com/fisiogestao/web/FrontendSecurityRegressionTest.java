package com.fisiogestao.web;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrontendSecurityRegressionTest {

    private static final Path RESOURCES = Path.of("src/main/resources");
    private static final Pattern INLINE_HANDLER = Pattern.compile("\\son(?:click|change|submit|load|error)\\s*=",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern INLINE_SCRIPT_WITHOUT_NONCE = Pattern.compile(
            "<script(?![^>]*\\bsrc=)(?![^>]*\\bnonce)[^>]*>", Pattern.CASE_INSENSITIVE);

    @Test
    void recursosFrontendNaoDevemUsarInnerHtmlOuHandlersInline() throws Exception {
        try (var files = Files.walk(RESOURCES)) {
            for (Path file : files.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".html") || path.toString().endsWith(".js"))
                    .toList()) {
                String content = Files.readString(file);
                assertFalse(content.matches("(?s).*\\.innerHTML\\s*=.*"), "innerHTML proibido em " + file);
                assertFalse(content.matches("(?s).*\\.outerHTML\\s*=.*"), "outerHTML proibido em " + file);
                assertFalse(content.matches("(?s).*\\.insertAdjacentHTML\\s*\\(.*"),
                        "insertAdjacentHTML proibido em " + file);
                assertFalse(content.matches("(?s).*\\bdocument\\.write(?:ln)?\\s*\\(.*"),
                        "document.write proibido em " + file);
                assertFalse(INLINE_HANDLER.matcher(content).find(), "handler inline proibido em " + file);
                if (file.toString().endsWith(".html")) {
                    assertFalse(INLINE_SCRIPT_WITHOUT_NONCE.matcher(content).find(), "script inline sem nonce em " + file);
                }
            }
        }
    }

    @Test
    void templatesDevemConterNonceParaScriptsInline() throws Exception {
        String navigation = Files.readString(RESOURCES.resolve("templates/fragments/app-navigation.html"));
        assertTrue(navigation.contains("nonce=${cspNonce}"));
    }
}
