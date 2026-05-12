package com.fisiogestao.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MigrationVersioningTest {

    private static final Pattern VERSION_PATTERN = Pattern.compile("V(\\d+)__.+\\.sql");
    private static final Set<Integer> LEGACY_SKIPPED_VERSIONS = Set.of(5);

    @Test
    void migrationsVersionadasDevemSerSequenciaisSemLacunas() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        List<Integer> versions = Arrays.stream(resolver.getResources("classpath:db/migration/V*.sql"))
                .map(Resource::getFilename)
                .map(this::extractVersion)
                .sorted()
                .toList();

        assertTrue(!versions.isEmpty(), "Nenhuma migration versionada foi encontrada.");
        int expectedVersion = 1;
        for (int actualVersion : versions) {
            while (LEGACY_SKIPPED_VERSIONS.contains(expectedVersion)) {
                expectedVersion++;
            }
            assertEquals(expectedVersion, actualVersion);
            expectedVersion++;
        }
    }

    private int extractVersion(String filename) {
        assertTrue(filename != null && !filename.isBlank(), "Migration sem nome de arquivo.");
        Matcher matcher = VERSION_PATTERN.matcher(filename);
        assertTrue(matcher.matches(), () -> "Nome de migration fora do padrao Flyway versionado: " + filename);
        return Integer.parseInt(matcher.group(1));
    }
}
