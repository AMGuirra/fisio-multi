package com.fisiogestao.auditoria.service;

import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.prontuario.ProntuarioAnexoEntity;
import com.fisiogestao.prontuario.ProntuarioSoapEntity;
import com.fisiogestao.prontuario.TipoRegistroProntuario;
import com.fisiogestao.solicitacoes.SolicitacaoAgendamentoEntity;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditSnapshotFactorySecurityTest {

    private final AuditSnapshotFactory factory = new AuditSnapshotFactory();

    @Test
    void snapshotPacienteNaoDeveDuplicarContatoOuObservacoes() {
        ClienteEntity paciente = ClienteEntity.builder()
                .nomeCompleto("Paciente Ficticio")
                .telefone("00000000000")
                .email("paciente@exemplo.invalid")
                .observacoes("Dado sensivel ficticio")
                .ativo(true)
                .build();

        Map<String, Object> snapshot = factory.snapshot(paciente);

        assertFalse(snapshot.containsKey("telefone"));
        assertFalse(snapshot.containsKey("email"));
        assertFalse(snapshot.containsKey("observacoes"));
        assertFalse(snapshot.containsKey("nomeCompleto"));
        assertEquals(true, snapshot.get("possuiTelefone"));
        assertFalse(factory.describe(paciente).contains("Paciente Ficticio"));
    }

    @Test
    void snapshotProntuarioDeveConterSomenteHashDoConteudoClinico() {
        String conteudoFicticio = "Conteudo clinico ficticio para teste";
        ProntuarioSoapEntity prontuario = ProntuarioSoapEntity.builder()
                .tipoRegistro(TipoRegistroProntuario.EVOLUCAO)
                .textoRegistro(conteudoFicticio)
                .build();

        Map<String, Object> snapshot = factory.snapshot(prontuario);

        assertFalse(snapshot.containsKey("textoRegistro"));
        assertTrue(snapshot.containsKey("conteudoClinicoHash"));
        assertNotEquals(conteudoFicticio, snapshot.get("conteudoClinicoHash"));
        assertEquals(64, snapshot.get("conteudoClinicoHash").toString().length());
    }

    @Test
    void snapshotSolicitacaoNaoDeveDuplicarDadosPessoaisOuEndereco() {
        SolicitacaoAgendamentoEntity solicitacao = SolicitacaoAgendamentoEntity.builder()
                .nomePaciente("Paciente Ficticio")
                .telefone("00000000000")
                .email("paciente@exemplo.invalid")
                .cpf("00000000000")
                .rua("Endereco Ficticio")
                .observacoes("Observacao ficticia")
                .build();

        Map<String, Object> snapshot = factory.snapshot(solicitacao);

        assertFalse(snapshot.containsKey("nomePaciente"));
        assertFalse(snapshot.containsKey("telefone"));
        assertFalse(snapshot.containsKey("email"));
        assertFalse(snapshot.containsKey("cpf"));
        assertFalse(snapshot.containsKey("rua"));
        assertFalse(snapshot.containsKey("observacoes"));
    }

    @Test
    void snapshotAnexoNaoDeveDuplicarNomeDescricaoOuBinario() {
        ProntuarioAnexoEntity anexo = ProntuarioAnexoEntity.builder()
                .nomeOriginal("documento-ficticio.pdf")
                .descricao("Descricao clinica ficticia")
                .dados(new byte[]{1, 2, 3})
                .build();

        Map<String, Object> snapshot = factory.snapshot(anexo);

        assertFalse(snapshot.containsKey("nomeOriginal"));
        assertFalse(snapshot.containsKey("descricao"));
        assertFalse(snapshot.containsKey("dados"));
        assertEquals(64, snapshot.get("nomeOriginalHash").toString().length());
    }
}
