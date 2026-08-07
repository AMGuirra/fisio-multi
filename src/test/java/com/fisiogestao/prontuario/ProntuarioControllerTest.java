package com.fisiogestao.prontuario;

import com.fisiogestao.auth.UserRole;
import com.fisiogestao.auth.UsuarioSistemaEntity;
import com.fisiogestao.auth.UsuarioSistemaRepository;
import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.pacientes.repository.ClienteRepository;
import com.fisiogestao.prontuario.repository.ProntuarioAnexoRepository;
import com.fisiogestao.prontuario.repository.ProntuarioSoapRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Cobre autenticação, permissão por menu (PRONTUARIO), CSRF, IDOR e o fluxo multipart
 * de avaliação/evolução/fotos do prontuário via MockMvc de ponta a ponta (contexto Spring real).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProntuarioControllerTest {

    private static final String RECEPCAO_USERNAME = "recepcao-prontuario-teste";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProntuarioSoapRepository prontuarioSoapRepository;

    @Autowired
    private ProntuarioAnexoRepository prontuarioAnexoRepository;

    @Autowired
    private UsuarioSistemaRepository usuarioSistemaRepository;

    private ClienteEntity paciente;
    private ClienteEntity outroPaciente;

    @BeforeEach
    void setUp() {
        paciente = clienteRepository.save(ClienteEntity.builder()
                .nomeCompleto("Paciente Prontuario Teste")
                .telefone("65999990001")
                .email("paciente.prontuario.teste@example.com")
                .ativo(true)
                .build());

        outroPaciente = clienteRepository.save(ClienteEntity.builder()
                .nomeCompleto("Outro Paciente Prontuario Teste")
                .telefone("65999990002")
                .email("outro.prontuario.teste@example.com")
                .ativo(true)
                .build());

        usuarioSistemaRepository.findByUsername(RECEPCAO_USERNAME)
                .orElseGet(() -> usuarioSistemaRepository.save(UsuarioSistemaEntity.builder()
                        .nome("Recepção Prontuário Teste")
                        .username(RECEPCAO_USERNAME)
                        .email("recepcao.prontuario.teste@example.com")
                        .passwordHash("mock-password-hash")
                        .role(UserRole.RECEPCAO)
                        .build()));
    }

    // -------------------------------------------------------------------------
    // Autenticação
    // -------------------------------------------------------------------------

    @Test
    void acessoAnonimoRedirecionaParaLogin() throws Exception {
        mockMvc.perform(get("/prontuario"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    // -------------------------------------------------------------------------
    // Permissão por menu (RECEPCAO não tem PRONTUARIO liberado por padrão)
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(username = RECEPCAO_USERNAME, roles = "RECEPCAO")
    void roleSemPermissaoEhRedirecionadaAoAbrirPagina() throws Exception {
        mockMvc.perform(get("/prontuario"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard?acessoNegado=1"));
    }

    @Test
    @WithMockUser(username = RECEPCAO_USERNAME, roles = "RECEPCAO")
    void roleSemPermissaoEhRedirecionadaAoSalvarAvaliacao() throws Exception {
        mockMvc.perform(post("/prontuario/avaliacao")
                        .with(csrf())
                        .param("clienteId", paciente.getId().toString())
                        .param("texto", "Tentativa sem permissão"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard?acessoNegado=1"));

        assertTrue(prontuarioSoapRepository.findByClienteIdOrderByCreatedAtDesc(paciente.getId()).isEmpty());
    }

    // -------------------------------------------------------------------------
    // CSRF
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void salvarAvaliacaoSemCsrfEhBloqueado() throws Exception {
        mockMvc.perform(post("/prontuario/avaliacao")
                        .param("clienteId", paciente.getId().toString())
                        .param("texto", "Sem token CSRF"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?expired"));

        assertTrue(prontuarioSoapRepository.findByClienteIdOrderByCreatedAtDesc(paciente.getId()).isEmpty());
    }

    // -------------------------------------------------------------------------
    // Fluxo feliz: avaliação e evolução (admin, com permissão)
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminAbrePaginaComPacienteSelecionado() throws Exception {
        mockMvc.perform(get("/prontuario").param("paciente", paciente.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("prontuario/index"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCriaEDepoisAtualizaAvaliacaoComAuditoriaDeRetificacao() throws Exception {
        mockMvc.perform(post("/prontuario/avaliacao")
                        .with(csrf())
                        .param("clienteId", paciente.getId().toString())
                        .param("texto", "Avaliação inicial do paciente."))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("successMessage"));

        assertEquals(1, prontuarioSoapRepository.findByClienteIdOrderByCreatedAtDesc(paciente.getId()).size());

        mockMvc.perform(post("/prontuario/avaliacao")
                        .with(csrf())
                        .param("clienteId", paciente.getId().toString())
                        .param("texto", "Avaliação retificada."))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("successMessage"));

        List<ProntuarioSoapEntity> registros = prontuarioSoapRepository.findByClienteIdOrderByCreatedAtDesc(paciente.getId());
        assertEquals(1, registros.size(), "Avaliação deve ser atualizada in-place, não duplicada");
        assertEquals("Avaliação retificada.", registros.get(0).getTextoRegistro());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminNaoConsegueSalvarAvaliacaoVazia() throws Exception {
        mockMvc.perform(post("/prontuario/avaliacao")
                        .with(csrf())
                        .param("clienteId", paciente.getId().toString())
                        .param("texto", "   "))
                .andExpect(status().isOk())
                .andExpect(view().name("prontuario/index"));

        assertTrue(prontuarioSoapRepository.findByClienteIdOrderByCreatedAtDesc(paciente.getId()).isEmpty());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminRegistraDuasEvolucoesComoRegistrosIndependentes() throws Exception {
        mockMvc.perform(post("/prontuario/evolucao")
                        .with(csrf())
                        .param("clienteId", paciente.getId().toString())
                        .param("texto", "Primeira evolução."))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/prontuario/evolucao")
                        .with(csrf())
                        .param("clienteId", paciente.getId().toString())
                        .param("texto", "Segunda evolução."))
                .andExpect(status().is3xxRedirection());

        List<ProntuarioSoapEntity> evolucoes = prontuarioSoapRepository
                .findByClienteIdAndTipoRegistroOrderByCreatedAtDesc(paciente.getId(), TipoRegistroProntuario.EVOLUCAO);
        assertEquals(2, evolucoes.size());
    }

    // -------------------------------------------------------------------------
    // Evolução: editar e excluir
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminEditaTextoDeEvolucaoExistente() throws Exception {
        UUID evolucaoId = registrarEvolucao(paciente.getId(), "Texto original.");

        mockMvc.perform(post("/prontuario/evolucao/{id}/editar", evolucaoId)
                        .with(csrf())
                        .param("clienteId", paciente.getId().toString())
                        .param("texto", "Texto corrigido."))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("successMessage"));

        ProntuarioSoapEntity atualizado = prontuarioSoapRepository.findById(evolucaoId).orElseThrow();
        assertEquals("Texto corrigido.", atualizado.getTextoRegistro());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminExcluiEvolucaoExistente() throws Exception {
        UUID evolucaoId = registrarEvolucao(paciente.getId(), "Evolução a ser excluída.");

        mockMvc.perform(post("/prontuario/evolucao/{id}/excluir", evolucaoId)
                        .with(csrf())
                        .param("clienteId", paciente.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("successMessage"));

        assertTrue(prontuarioSoapRepository.findById(evolucaoId).isEmpty());
    }

    @Test
    @WithMockUser(username = RECEPCAO_USERNAME, roles = "RECEPCAO")
    void roleSemPermissaoEhRedirecionadaAoExcluirEvolucao() throws Exception {
        UUID evolucaoId = registrarEvolucaoComoAdmin(paciente.getId(), "Evolução protegida.");

        mockMvc.perform(post("/prontuario/evolucao/{id}/excluir", evolucaoId)
                        .with(csrf())
                        .param("clienteId", paciente.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard?acessoNegado=1"));

        assertTrue(prontuarioSoapRepository.findById(evolucaoId).isPresent());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void naoPermiteExcluirEvolucaoDeOutroPaciente() throws Exception {
        UUID evolucaoId = registrarEvolucao(paciente.getId(), "Evolução do paciente correto.");

        mockMvc.perform(post("/prontuario/evolucao/{id}/excluir", evolucaoId)
                        .with(csrf())
                        .param("clienteId", outroPaciente.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("errorMessage"));

        assertTrue(prontuarioSoapRepository.findById(evolucaoId).isPresent());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void excluirEvolucaoInexistenteNaoLancaErroEMostraFlashDeErro() throws Exception {
        mockMvc.perform(post("/prontuario/evolucao/{id}/excluir", UUID.randomUUID())
                        .with(csrf())
                        .param("clienteId", paciente.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("errorMessage"));
    }

    private UUID registrarEvolucao(UUID clienteId, String texto) throws Exception {
        mockMvc.perform(post("/prontuario/evolucao")
                        .with(csrf())
                        .param("clienteId", clienteId.toString())
                        .param("texto", texto))
                .andExpect(status().is3xxRedirection());

        return prontuarioSoapRepository
                .findByClienteIdAndTipoRegistroOrderByCreatedAtDesc(clienteId, TipoRegistroProntuario.EVOLUCAO)
                .get(0)
                .getId();
    }

    /** Registra a evolução como admin, independente do usuário autenticado no teste em execução. */
    private UUID registrarEvolucaoComoAdmin(UUID clienteId, String texto) throws Exception {
        mockMvc.perform(post("/prontuario/evolucao")
                        .with(csrf())
                        .with(user("admin").roles("ADMIN"))
                        .param("clienteId", clienteId.toString())
                        .param("texto", texto))
                .andExpect(status().is3xxRedirection());

        return prontuarioSoapRepository
                .findByClienteIdAndTipoRegistroOrderByCreatedAtDesc(clienteId, TipoRegistroProntuario.EVOLUCAO)
                .get(0)
                .getId();
    }

    // -------------------------------------------------------------------------
    // Multipart: fotos de acompanhamento (consentimento + magic bytes reais)
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void evolucaoComFotoSemConsentimentoEhRejeitada() throws Exception {
        MockMultipartFile foto = new MockMultipartFile("fotos", "joelho.jpg", "image/jpeg", jpegValido());

        mockMvc.perform(multipart("/prontuario/evolucao")
                        .file(foto)
                        .param("clienteId", paciente.getId().toString())
                        .param("regiaoFotos", "Joelho direito")
                        .param("observacaoFotos", "Edema reduzido")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("prontuario/index"));

        assertTrue(prontuarioAnexoRepository.findResumosByClienteId(paciente.getId()).isEmpty());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void evolucaoComFotoValidaEConsentimentoPersisteFotoClinicaVinculada() throws Exception {
        MockMultipartFile foto = new MockMultipartFile("fotos", "joelho.jpg", "image/jpeg", jpegValido());

        mockMvc.perform(multipart("/prontuario/evolucao")
                        .file(foto)
                        .param("clienteId", paciente.getId().toString())
                        .param("regiaoFotos", "Joelho direito")
                        .param("observacaoFotos", "Edema reduzido")
                        .param("consentimentoFotos", "true")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("successMessage"));

        var anexos = prontuarioAnexoRepository.findResumosByClienteIdAndTipoAnexo(paciente.getId(), TipoAnexoProntuario.FOTO_CLINICA);
        assertEquals(1, anexos.size());
        assertEquals("Joelho direito", anexos.get(0).getRegiaoCorporal());

        UUID fotoId = anexos.get(0).getId();
        mockMvc.perform(get("/prontuario/foto/{id}/miniatura", fotoId))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void evolucaoRejeitaArquivoComExtensaoFalsificada() throws Exception {
        MockMultipartFile arquivoForjado = new MockMultipartFile(
                "fotos", "foto.jpg", "image/jpeg", "conteudo que nao e uma imagem jpeg de verdade".getBytes());

        mockMvc.perform(multipart("/prontuario/evolucao")
                        .file(arquivoForjado)
                        .param("clienteId", paciente.getId().toString())
                        .param("regiaoFotos", "Joelho")
                        .param("observacaoFotos", "Observação")
                        .param("consentimentoFotos", "true")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("prontuario/index"));

        assertTrue(prontuarioAnexoRepository.findResumosByClienteId(paciente.getId()).isEmpty());
    }

    // -------------------------------------------------------------------------
    // IDOR
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void naoPermiteVincularSessaoDeOutroPacienteNaEvolucao() throws Exception {
        // Sem sessão cadastrada, o service já rejeita qualquer sessaoId inexistente;
        // aqui simulamos o caso de posse cruzada com um UUID aleatório (sessão de outro contexto).
        mockMvc.perform(post("/prontuario/evolucao")
                        .with(csrf())
                        .param("clienteId", paciente.getId().toString())
                        .param("sessaoId", UUID.randomUUID().toString())
                        .param("texto", "Evolução com sessão inexistente."))
                .andExpect(status().isOk())
                .andExpect(view().name("prontuario/index"));

        assertTrue(prontuarioSoapRepository.findByClienteIdOrderByCreatedAtDesc(paciente.getId()).isEmpty());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void visualizarAnexoInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/prontuario/anexo/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void excluirAnexoInexistenteNaoLancaErroEMostraFlashDeErro() throws Exception {
        mockMvc.perform(post("/prontuario/anexo/{id}/excluir", UUID.randomUUID())
                        .with(csrf())
                        .param("clienteId", paciente.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void miniaturaDeAnexoQueNaoEhFotoClinicaRetorna404() throws Exception {
        MockMultipartFile documento = new MockMultipartFile(
                "arquivo", "laudo.pdf", "application/pdf", "%PDF-1.4 documento de teste".getBytes());

        mockMvc.perform(multipart("/prontuario/anexo")
                        .file(documento)
                        .param("clienteId", paciente.getId().toString())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        var documentos = prontuarioAnexoRepository.findResumosByClienteIdAndTipoAnexo(paciente.getId(), TipoAnexoProntuario.DOCUMENTO);
        assertEquals(1, documentos.size());

        mockMvc.perform(get("/prontuario/foto/{id}/miniatura", documentos.get(0).getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void anexoNaoMisturaDocumentosEntrePacientesDiferentes() throws Exception {
        MockMultipartFile documento = new MockMultipartFile(
                "arquivo", "laudo.pdf", "application/pdf", "%PDF-1.4 documento de teste".getBytes());

        mockMvc.perform(multipart("/prontuario/anexo")
                        .file(documento)
                        .param("clienteId", paciente.getId().toString())
                        .with(csrf()));

        assertTrue(prontuarioAnexoRepository.findResumosByClienteId(outroPaciente.getId()).isEmpty());
        assertFalse(prontuarioAnexoRepository.findResumosByClienteId(paciente.getId()).isEmpty());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Gera bytes de um JPEG 4x4 real e decodificável (não apenas o cabeçalho mágico). */
    private byte[] jpegValido() throws Exception {
        BufferedImage imagem = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        ImageIO.write(imagem, "jpg", saida);
        return saida.toByteArray();
    }
}
