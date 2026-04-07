package com.fisiogestao.solicitacoes;

import com.fisiogestao.agenda.repository.HorarioBloqueadoRepository;
import com.fisiogestao.agenda.service.AgendaService;
import com.fisiogestao.pacientes.PacoteSessaoStatus;
import com.fisiogestao.pacientes.form.PacoteSessaoForm;
import com.fisiogestao.pacientes.service.PacoteSessaoService;
import com.fisiogestao.shared.web.BusinessException;
import com.fisiogestao.solicitacoes.service.SolicitacaoAgendamentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/solicitacoes")
@RequiredArgsConstructor
public class SolicitacaoFluxoApiController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final SolicitacaoAgendamentoService solicitacaoService;
    private final PacoteSessaoService pacoteSessaoService;
    private final AgendaService agendaService;
    private final HorarioBloqueadoRepository horarioBloqueadoRepository;

    // ── Erro de negócio → 422 com JSON { "message": "..." } ──────────────────
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Map<String, String> handleBusinessException(BusinessException ex) {
        return Map.of("message", ex.getMessage());
    }

    // ── GET /api/solicitacoes/{id}/fluxo ─────────────────────────────────────
    // Retorna o estado completo do wizard: paciente, plano e disponibilidade
    @GetMapping("/{id}/fluxo")
    public FluxoStatusResponse verificarFluxo(@PathVariable UUID id) {
        SolicitacaoAgendamentoEntity sol = solicitacaoService.buscarPorId(id);

        if (sol.getStatus() != SolicitacaoStatus.PENDENTE) {
            throw new BusinessException("Esta solicitação não está mais pendente.");
        }

        // ── Etapa 1: paciente ─────────────────────────────────────────────────
        boolean pacienteEncontrado = sol.getCliente() != null;
        UUID clienteId  = pacienteEncontrado ? sol.getCliente().getId()           : null;
        String clienteNome = pacienteEncontrado ? sol.getCliente().getNomeCompleto() : null;

        // ── Etapa 2: plano ────────────────────────────────────────────────────
        boolean planoDisponivel = false;
        List<PlanoDto> planos   = List.of();
        if (pacienteEncontrado) {
            LocalDate hoje = LocalDate.now();
            List<PlanoDto> ativos = pacoteSessaoService.listarPorCliente(clienteId).stream()
                    .filter(p -> p.getStatus() == PacoteSessaoStatus.ATIVO
                            && (p.getDataValidade() == null || !p.getDataValidade().isBefore(hoje))
                            && p.getSessoesConsumidas() < p.getSessoesContratadas())
                    .map(p -> new PlanoDto(
                            p.getId(),
                            p.getNomePacote(),
                            p.getSessoesContratadas() - p.getSessoesConsumidas()))
                    .toList();
            planoDisponivel = !ativos.isEmpty();
            planos = ativos;
        }

        // ── Etapa 3: disponibilidade do horário ───────────────────────────────
        boolean horarioDisponivel     = false;
        String mensagemDisponibilidade = null;
        if (pacienteEncontrado) {
            UUID bloqueioId = horarioBloqueadoRepository
                    .findBySolicitacaoId(id)
                    .map(b -> b.getId())
                    .orElse(null);
            try {
                agendaService.validarHorarioDisponivel(
                        sol.getDataHoraInicio(),
                        sol.getDataHoraFim(),
                        bloqueioId,
                        null,
                        true   // permite horário passado (solicitações antigas)
                );
                horarioDisponivel = true;
            } catch (BusinessException ex) {
                mensagemDisponibilidade = ex.getMessage();
            }
        }

        return new FluxoStatusResponse(
                sol.getId(),
                sol.getNomePaciente(),
                sol.getTelefone(),
                sol.getEmail(),
                sol.getCpf(),
                sol.getDataHoraInicio().format(FMT),
                sol.getLocalAtendimentoDescricao(),
                sol.getObservacoes(),
                sol.getCep(),
                sol.getRua(),
                sol.getNumero(),
                sol.getBairro(),
                sol.getCidade(),
                sol.getEstado(),
                sol.getEnderecoResumo(),
                new PacienteInfo(pacienteEncontrado, clienteId, clienteNome),
                new PlanoInfo(planoDisponivel, clienteId, planos),
                new DisponibilidadeInfo(horarioDisponivel, mensagemDisponibilidade)
        );
    }

    // ── POST /api/solicitacoes/{id}/fluxo/criar-plano ─────────────────────────
    // Criação rápida de plano de tratamento a partir do wizard
    @PostMapping("/{id}/fluxo/criar-plano")
    public PlanoCriadoResponse criarPlano(
            @PathVariable UUID id,
            @RequestBody CriarPlanoRequest body
    ) {
        SolicitacaoAgendamentoEntity sol = solicitacaoService.buscarPorId(id);

        if (sol.getStatus() != SolicitacaoStatus.PENDENTE) {
            throw new BusinessException("Esta solicitação não está mais pendente.");
        }
        if (sol.getCliente() == null) {
            throw new BusinessException("Cadastre o paciente antes de criar o plano de tratamento.");
        }
        if (body.nomePacote() == null || body.nomePacote().isBlank()) {
            throw new BusinessException("Informe o nome do plano de tratamento.");
        }
        if (body.sessoesContratadas() == null || body.sessoesContratadas() < 1) {
            throw new BusinessException("O plano de tratamento precisa ter ao menos uma sessão.");
        }

        PacoteSessaoForm form = new PacoteSessaoForm();
        form.setNomePacote(body.nomePacote().trim());
        form.setSessoesContratadas(body.sessoesContratadas());
        form.setValorTotal(body.valorTotal() != null ? body.valorTotal() : BigDecimal.ZERO);
        form.setDataInicio(body.dataInicio() != null ? body.dataInicio() : LocalDate.now());
        form.setDataValidade(body.dataValidade());

        var plano = pacoteSessaoService.criar(sol.getCliente().getId(), form);

        return new PlanoCriadoResponse(
                plano.getId(),
                plano.getNomePacote(),
                plano.getSessoesContratadas() - plano.getSessoesConsumidas()
        );
    }

    // ── DTOs ──────────────────────────────────────────────────────────────────

    public record FluxoStatusResponse(
            UUID   solicitacaoId,
            String nomePaciente,
            String telefone,
            String email,
            String cpf,
            String dataHoraFormatada,
            String tipoAtendimento,
            String observacoes,
            String cep,
            String rua,
            String numero,
            String bairro,
            String cidade,
            String estado,
            String enderecoResumo,
            PacienteInfo       paciente,
            PlanoInfo          plano,
            DisponibilidadeInfo disponibilidade
    ) {}

    public record PacienteInfo(
            boolean encontrado,
            UUID    clienteId,
            String  nome
    ) {}

    public record PlanoInfo(
            boolean       disponivel,
            UUID          clienteId,
            List<PlanoDto> planos
    ) {}

    public record PlanoDto(
            UUID   id,
            String nomePacote,
            int    sessoesDisponiveis
    ) {}

    public record DisponibilidadeInfo(
            boolean disponivel,
            String  mensagem
    ) {}

    public record CriarPlanoRequest(
            String     nomePacote,
            Integer    sessoesContratadas,
            BigDecimal valorTotal,
            LocalDate  dataInicio,
            LocalDate  dataValidade
    ) {}

    public record PlanoCriadoResponse(
            UUID   id,
            String nomePacote,
            int    sessoesDisponiveis
    ) {}
}
