package com.fisiogestao.prontuario.service;

import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.agenda.repository.SessaoRepository;
import com.fisiogestao.pacientes.service.PacienteService;
import com.fisiogestao.prontuario.ProntuarioSoapEntity;
import com.fisiogestao.prontuario.form.ProntuarioSoapForm;
import com.fisiogestao.prontuario.repository.ProntuarioSoapRepository;
import com.fisiogestao.shared.web.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProntuarioService {

    private final ProntuarioSoapRepository prontuarioSoapRepository;
    private final PacienteService pacienteService;
    private final SessaoRepository sessaoRepository;

    public List<ProntuarioSoapEntity> listarPorPaciente(UUID clienteId) {
        return prontuarioSoapRepository.findByClienteIdOrderByCreatedAtDesc(clienteId);
    }

    @Transactional
    public ProntuarioSoapEntity registrarSoap(ProntuarioSoapForm form, String usuario) {
        SessaoEntity sessao = null;
        if (form.getSessaoId() != null) {
            sessao = sessaoRepository.findById(form.getSessaoId())
                    .orElseThrow(() -> new BusinessException("Sessão vinculada ao prontuário não encontrada."));
        }

        ProntuarioSoapEntity prontuario = ProntuarioSoapEntity.builder()
                .cliente(pacienteService.buscarPorId(form.getClienteId()))
                .sessao(sessao)
                .subjetivo(form.getSubjetivo().trim())
                .objetivo(form.getObjetivo().trim())
                .avaliacao(form.getAvaliacao().trim())
                .plano(form.getPlano().trim())
                .criadoPor(usuario)
                .build();
        return prontuarioSoapRepository.save(prontuario);
    }
}
