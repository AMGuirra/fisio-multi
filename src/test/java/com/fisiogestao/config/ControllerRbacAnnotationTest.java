package com.fisiogestao.config;

import com.fisiogestao.agenda.AgendamentosApiController;
import com.fisiogestao.pacientes.PacientesApiController;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ControllerRbacAnnotationTest {

    @Test
    void pacientesApiDeveExigirPermissaoDoModulo() {
        assertPreAuthorize(PacientesApiController.class, "@menuAccessSecurity.canAccess('PACIENTES')");
    }

    @Test
    void agendamentosApiDeveExigirPermissaoDoModulo() {
        assertPreAuthorize(AgendamentosApiController.class, "@menuAccessSecurity.canAccess('AGENDA')");
    }

    private void assertPreAuthorize(Class<?> controller, String expressaoEsperada) {
        PreAuthorize annotation = controller.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation);
        assertEquals(expressaoEsperada, annotation.value());
    }
}
