package com.fisiogestao.pacientes.service;

import com.fisiogestao.agenda.SessaoStatus;
import com.fisiogestao.agenda.repository.SessaoRepository;
import com.fisiogestao.financeiro.service.FinanceiroService;
import com.fisiogestao.pacientes.PacoteSessaoEntity;
import com.fisiogestao.pacientes.PacoteSessaoStatus;
import com.fisiogestao.pacientes.form.PacoteConsumoForm;
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

    private static final List<SessaoStatus> STATUS_SESSAO_COM_PACOTE_ATIVO = List.of(
            SessaoStatus.AGENDADA,
            SessaoStatus.REALIZADA
    );

    private final PacoteSessaoRepository pacoteSessaoRepository;
    private final PacienteService pacienteService;
    private final FinanceiroService financeiroService;
    private final SessaoRepository sessaoRepository;

    public List<PacoteSessaoEntity> listarPorCliente(UUID clienteId) {
        return pacoteSessaoRepository.findByClienteIdOrderByCreatedAtDesc(clienteId);
    }

    public List<PacoteSessaoEntity> listarDisponiveisParaAgenda() {
        return pacoteSessaoRepository.findByStatusOrderByCreatedAtDesc(PacoteSessaoStatus.ATIVO).stream()
                .filter(this::isDisponivel)
                .toList();
    }

    public Optional<PacoteSessaoEntity> buscarPorId(UUID id) {
        return pacoteSessaoRepository.findById(id);
    }

    public Optional<PacoteSessaoEntity> buscarPrimeiroDisponivel(UUID clienteId) {
        return listarPorCliente(clienteId).stream()
                .filter(this::isDisponivel)
                .sorted(comparadorDisponibilidade())
                .findFirst();
    }

    @Transactional
    public PacoteSessaoEntity criar(UUID clienteId, PacoteSessaoForm form) {
        validarPeriodo(form.getDataInicio(), form.getDataValidade());

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
    public PacoteSessaoEntity criarViaCrud(PacoteConsumoForm form) {
        validarPeriodo(form.getDataInicio(), form.getDataValidade());

        PacoteSessaoEntity pacote = PacoteSessaoEntity.builder()
                .cliente(pacienteService.buscarPorId(form.getClienteId()))
                .nomePacote(form.getNomePacote().trim())
                .sessoesContratadas(form.getSessoesContratadas())
                .sessoesConsumidas(0)
                .valorTotal(form.getValorTotal())
                .dataInicio(form.getDataInicio())
                .dataValidade(form.getDataValidade())
                .status(statusAjustado(form.getStatus(), 0, form.getSessoesContratadas(), form.getDataValidade()))
                .build();
        PacoteSessaoEntity salvo = pacoteSessaoRepository.save(pacote);
        financeiroService.registrarReceitaPacote(salvo);
        return salvo;
    }

    @Transactional
    public PacoteSessaoEntity atualizarViaCrud(UUID pacoteId, PacoteConsumoForm form) {
        PacoteSessaoEntity pacote = pacoteSessaoRepository.findById(pacoteId)
                .orElseThrow(() -> new BusinessException("Pacote de sessões não encontrado."));

        validarPeriodo(form.getDataInicio(), form.getDataValidade());
        validarConsumo(pacote.getSessoesConsumidas(), form.getSessoesContratadas());
        validarCancelamentoPorStatus(pacote, form.getStatus());

        pacote.setCliente(pacienteService.buscarPorId(form.getClienteId()));
        pacote.setNomePacote(form.getNomePacote().trim());
        pacote.setSessoesContratadas(form.getSessoesContratadas());
        pacote.setValorTotal(form.getValorTotal());
        pacote.setDataInicio(form.getDataInicio());
        pacote.setDataValidade(form.getDataValidade());
        pacote.setStatus(statusAjustado(
                form.getStatus(),
                pacote.getSessoesConsumidas(),
                form.getSessoesContratadas(),
                form.getDataValidade()
        ));
        return pacoteSessaoRepository.save(pacote);
    }

    @Transactional
    public void cancelar(UUID pacoteId) {
        PacoteSessaoEntity pacote = pacoteSessaoRepository.findById(pacoteId)
                .orElseThrow(() -> new BusinessException("Pacote de sessões não encontrado."));
        validarCancelamento(pacote);
        pacote.setStatus(PacoteSessaoStatus.CANCELADO);
        pacoteSessaoRepository.save(pacote);
    }

    @Transactional
    public void reservarSessao(UUID pacoteId) {
        PacoteSessaoEntity pacote = validarDisponivel(pacoteId);
        pacote.setSessoesConsumidas(pacote.getSessoesConsumidas() + 1);
        pacote.setStatus(statusAjustado(
                pacote.getStatus(),
                pacote.getSessoesConsumidas(),
                pacote.getSessoesContratadas(),
                pacote.getDataValidade()
        ));
        pacoteSessaoRepository.save(pacote);
    }

    @Transactional
    public void estornarSessao(UUID pacoteId) {
        PacoteSessaoEntity pacote = pacoteSessaoRepository.findById(pacoteId)
                .orElseThrow(() -> new BusinessException("Pacote de sessões não encontrado."));
        if (pacote.getSessoesConsumidas() <= 0) {
            return;
        }
        pacote.setSessoesConsumidas(pacote.getSessoesConsumidas() - 1);
        pacote.setStatus(statusAjustado(
                PacoteSessaoStatus.ATIVO,
                pacote.getSessoesConsumidas(),
                pacote.getSessoesContratadas(),
                pacote.getDataValidade()
        ));
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

    public PacoteSessaoEntity resolverPacoteParaSessao(UUID clienteId, UUID pacoteId, UUID pacoteAtualId) {
        if (pacoteId == null) {
            return buscarPrimeiroDisponivel(clienteId)
                    .orElseThrow(() -> new BusinessException(
                            "O paciente não possui pacote de sessões disponível. Cadastre ou renove um pacote antes de confirmar a sessão."
                    ));
        }

        PacoteSessaoEntity pacote = buscarPorId(pacoteId)
                .orElseThrow(() -> new BusinessException("Pacote de sessões não encontrado."));
        if (!pacote.getCliente().getId().equals(clienteId)) {
            throw new BusinessException("O pacote selecionado não pertence ao paciente informado.");
        }
        if (pacoteAtualId != null && pacoteAtualId.equals(pacoteId)) {
            return pacote;
        }
        return validarDisponivel(pacoteId);
    }

    public int calcularSessoesDisponiveis(PacoteSessaoEntity pacote) {
        return Math.max(0, pacote.getSessoesContratadas() - pacote.getSessoesConsumidas());
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

    private void validarPeriodo(LocalDate dataInicio, LocalDate dataValidade) {
        if (dataValidade != null && dataValidade.isBefore(dataInicio)) {
            throw new BusinessException("A validade do pacote não pode ser anterior à data inicial.");
        }
    }

    private void validarConsumo(Integer sessoesConsumidas, Integer sessoesContratadas) {
        if (sessoesConsumidas > sessoesContratadas) {
            throw new BusinessException("As sessões consumidas não podem ser maiores do que as sessões contratadas.");
        }
    }

    private void validarCancelamento(PacoteSessaoEntity pacote) {
        if (sessaoRepository.existsByPacoteSessaoIdAndStatusIn(pacote.getId(), STATUS_SESSAO_COM_PACOTE_ATIVO)) {
            throw new BusinessException("Este pacote possui sessões confirmadas ou realizadas vinculadas e não pode ser cancelado.");
        }
    }

    private void validarCancelamentoPorStatus(PacoteSessaoEntity pacote, PacoteSessaoStatus statusSolicitado) {
        if (statusSolicitado == PacoteSessaoStatus.CANCELADO) {
            validarCancelamento(pacote);
        }
    }

    private Comparator<PacoteSessaoEntity> comparadorDisponibilidade() {
        return Comparator
                .comparing((PacoteSessaoEntity pacote) -> pacote.getDataValidade() == null)
                .thenComparing(PacoteSessaoEntity::getDataValidade, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(PacoteSessaoEntity::getDataInicio)
                .thenComparing(PacoteSessaoEntity::getCreatedAt);
    }

    private PacoteSessaoStatus statusAjustado(
            PacoteSessaoStatus statusSolicitado,
            Integer sessoesConsumidas,
            Integer sessoesContratadas,
            LocalDate dataValidade
    ) {
        if (statusSolicitado == PacoteSessaoStatus.CANCELADO) {
            return PacoteSessaoStatus.CANCELADO;
        }
        if (sessoesConsumidas >= sessoesContratadas) {
            return PacoteSessaoStatus.CONCLUIDO;
        }
        if (dataValidade != null && dataValidade.isBefore(LocalDate.now())) {
            return PacoteSessaoStatus.EXPIRADO;
        }
        return PacoteSessaoStatus.ATIVO;
    }
}
