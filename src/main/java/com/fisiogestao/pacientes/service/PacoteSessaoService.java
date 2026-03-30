package com.fisiogestao.pacientes.service;

import com.fisiogestao.financeiro.service.FinanceiroService;
import com.fisiogestao.pacientes.PacoteSessaoEntity;
import com.fisiogestao.pacientes.PacoteSessaoStatus;
import com.fisiogestao.pacientes.form.PacoteSessaoForm;
import com.fisiogestao.pacientes.repository.PacoteSessaoRepository;
import com.fisiogestao.shared.web.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PacoteSessaoService {

    private final PacoteSessaoRepository pacoteSessaoRepository;
    private final PacienteService pacienteService;
    private final FinanceiroService financeiroService;

    public List<PacoteSessaoEntity> listarPorCliente(UUID clienteId) {
        return pacoteSessaoRepository.findByClienteIdOrderByCreatedAtDesc(clienteId);
    }

    public Optional<PacoteSessaoEntity> buscarPorId(UUID id) {
        return pacoteSessaoRepository.findById(id);
    }

    public Optional<PacoteSessaoEntity> buscarPrimeiroDisponivel(UUID clienteId) {
        return listarPorCliente(clienteId).stream()
                .filter(this::isDisponivel)
                .sorted(Comparator.comparing(PacoteSessaoEntity::getCreatedAt).reversed())
                .findFirst();
    }

    @Transactional
    public PacoteSessaoEntity criar(UUID clienteId, PacoteSessaoForm form) {
        if (form.getDataValidade() != null && form.getDataValidade().isBefore(form.getDataInicio())) {
            throw new BusinessException("A validade do pacote não pode ser anterior à data inicial.");
        }

        PacoteSessaoEntity pacote = PacoteSessaoEntity.builder()
                .cliente(pacienteService.buscarPorId(clienteId))
                .nomePacote(form.getNomePacote().trim())
                .sessoesContratadas(form.getSessoesContratadas())
                .sessoesConsumidas(0)
                .valorTotal(form.getValorTotal())
                .dataInicio(form.getDataInicio())
                .dataValidade(form.getDataValidade())
                .status(PacoteSessaoStatus.ATIVO)
                .build();
        PacoteSessaoEntity salvo = pacoteSessaoRepository.save(pacote);
        financeiroService.registrarReceitaPacote(salvo);
        return salvo;
    }

    @Transactional
    public void consumirSessao(UUID pacoteId) {
        PacoteSessaoEntity pacote = validarDisponivel(pacoteId);
        pacote.setSessoesConsumidas(pacote.getSessoesConsumidas() + 1);
        if (pacote.getSessoesConsumidas() >= pacote.getSessoesContratadas()) {
            pacote.setStatus(PacoteSessaoStatus.CONCLUIDO);
        }
        pacoteSessaoRepository.save(pacote);
    }

    public PacoteSessaoEntity validarDisponivel(UUID pacoteId) {
        PacoteSessaoEntity pacote = pacoteSessaoRepository.findById(pacoteId)
                .orElseThrow(() -> new BusinessException("Pacote de sessões não encontrado."));
        if (!isDisponivel(pacote)) {
            throw new BusinessException("O pacote selecionado não possui sessões disponíveis.");
        }
        return pacote;
    }

    private boolean isDisponivel(PacoteSessaoEntity pacote) {
        if (pacote.getStatus() != PacoteSessaoStatus.ATIVO) {
            return false;
        }
        if (pacote.getDataValidade() != null && pacote.getDataValidade().isBefore(LocalDate.now())) {
            return false;
        }
        return pacote.getSessoesConsumidas() < pacote.getSessoesContratadas();
    }
}
