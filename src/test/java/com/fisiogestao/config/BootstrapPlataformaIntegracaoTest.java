package com.fisiogestao.config;

import com.fisiogestao.auth.UserRole;
import com.fisiogestao.auth.UsuarioSistemaEntity;
import com.fisiogestao.empresa.EmpresaLegada;
import com.fisiogestao.support.MultiEmpresaTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Bootstrap na aplicação real: superadmin sem empresa, idempotente, e empresa legada garantida. */
class BootstrapPlataformaIntegracaoTest extends MultiEmpresaTestBase {

    @Autowired private DataInitializer dataInitializer;

    @Test
    void bootstrapEhIdempotenteENaoAlteraSenhaExistente() {
        UsuarioSistemaEntity antes = usuarioRepository.findByEmpresaIdIsNullAndUsername("superadmin").orElseThrow();
        assertEquals(UserRole.SUPER_ADMIN, antes.getRole());
        assertNull(antes.getEmpresaId());

        antes.setPasswordHash(passwordEncoder.encode("SenhaTrocadaPeloUsuario1"));
        usuarioRepository.save(antes);

        dataInitializer.run();
        dataInitializer.run();

        UsuarioSistemaEntity depois = usuarioRepository.findByEmpresaIdIsNullAndUsername("superadmin").orElseThrow();
        assertEquals(antes.getId(), depois.getId());
        assertTrue(passwordEncoder.matches("SenhaTrocadaPeloUsuario1", depois.getPasswordHash()));
        assertEquals(1, empresaRepository.findAll().stream().filter(e -> EmpresaLegada.SLUG.equals(e.getSlug())).count());

        // Restaura a senha usada pelos demais testes.
        depois.setPasswordHash(passwordEncoder.encode("admin"));
        usuarioRepository.save(depois);
    }
}
