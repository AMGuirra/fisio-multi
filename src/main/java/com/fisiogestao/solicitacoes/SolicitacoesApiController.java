package com.fisiogestao.solicitacoes;

import com.fisiogestao.solicitacoes.form.AprovacaoSolicitacaoForm;
import com.fisiogestao.solicitacoes.form.EditarSolicitacaoForm;
import com.fisiogestao.solicitacoes.form.ReagendarSolicitacaoForm;
import com.fisiogestao.solicitacoes.service.SolicitacaoAgendamentoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/solicitacoes")
@RequiredArgsConstructor
public class SolicitacoesApiController {

    private final SolicitacaoAgendamentoService solicitacaoAgendamentoService;

    @GetMapping
    public List<SolicitacaoDto> listar(
            @RequestParam(name = "status", required = false) SolicitacaoStatus status,
            @RequestParam(name = "inicio", required = false) LocalDate inicio,
            @RequestParam(name = "fim", required = false) LocalDate fim
    ) {
        List<SolicitacaoAgendamentoEntity> solicitacoes;
        if (inicio != null && fim != null && !fim.isBefore(inicio)) {
            solicitacoes = solicitacaoAgendamentoService.listarNoPeriodo(inicio, fim.plusDays(1));
        } else if (status == SolicitacaoStatus.PENDENTE) {
            solicitacoes = solicitacaoAgendamentoService.listarPendentes();
        } else {
            solicitacoes = solicitacaoAgendamentoService.listarTodas();
        }

        return solicitacoes.stream()
                .filter(sol -> status == null || sol.getStatus() == status)
                .sorted(Comparator.comparing(SolicitacaoAgendamentoEntity::getDataHoraInicio))
                .map(this::toDto)
                .toList();
    }

    @GetMapping("/{id}")
    public SolicitacaoDto detalhar(@PathVariable UUID id) {
        return toDto(solicitacaoAgendamentoService.buscarPorId(id));
    }

    @PostMapping("/{id}/aprovar")
    public SolicitacaoDto aprovar(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) AprovarRequest body
    ) {
        AprovacaoSolicitacaoForm form = new AprovacaoSolicitacaoForm();
        if (body != null) {
            form.setPlanoSessaoId(body.planoSessaoId());
        }
        solicitacaoAgendamentoService.aprovarSolicitacao(id, form);
        return toDto(solicitacaoAgendamentoService.buscarPorId(id));
    }

    @PostMapping("/{id}/cadastrar")
    public SolicitacaoDto cadastrar(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) CadastrarPacienteRequest body
    ) {
        AprovacaoSolicitacaoForm form = new AprovacaoSolicitacaoForm();
        if (body != null) {
            form.setNomeCompleto(body.nomeCompleto());
            form.setTelefone(body.telefone());
            form.setEmail(body.email());
            form.setCpf(body.cpf());
            form.setCep(body.cep());
            form.setRua(body.rua());
            form.setNumero(body.numero());
            form.setBairro(body.bairro());
            form.setCidade(body.cidade());
            form.setEstado(body.estado());
        }
        solicitacaoAgendamentoService.cadastrarPaciente(id, form);
        return toDto(solicitacaoAgendamentoService.buscarPorId(id));
    }

    @PostMapping("/{id}/rejeitar")
    public SolicitacaoDto rejeitar(@PathVariable UUID id) {
        solicitacaoAgendamentoService.rejeitarSolicitacao(id);
        return toDto(solicitacaoAgendamentoService.buscarPorId(id));
    }

    @PostMapping("/{id}/editar")
    public SolicitacaoDto editar(
            @PathVariable UUID id,
            @Valid @RequestBody EditarRequest body
    ) {
        EditarSolicitacaoForm form = new EditarSolicitacaoForm();
        form.setNomePaciente(body.nomePaciente());
        form.setTelefone(body.telefone());
        form.setEmail(body.email());
        form.setData(body.data());
        form.setLocalAtendimento(body.localAtendimento());
        form.setObservacoes(body.observacoes());
        form.setMotivo(body.motivo());
        try {
            form.setHorario(java.time.LocalTime.parse(body.horario()));
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(BAD_REQUEST, "horario inválido. Use o formato HH:mm.");
        }
        solicitacaoAgendamentoService.editarSolicitacao(id, form);
        return toDto(solicitacaoAgendamentoService.buscarPorId(id));
    }

    @PostMapping("/{id}/reagendar")
    public SolicitacaoDto reagendar(
            @PathVariable UUID id,
            @Valid @RequestBody ReagendarRequest body
    ) {
        ReagendarSolicitacaoForm form = new ReagendarSolicitacaoForm();
        form.setData(body.data());
        form.setMotivo(body.motivo());
        try {
            form.setHorario(LocalTime.parse(body.horario()));
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(BAD_REQUEST, "horario inválido. Use o formato HH:mm.");
        }

        solicitacaoAgendamentoService.reagendarSolicitacao(id, form);
        return toDto(solicitacaoAgendamentoService.buscarPorId(id));
    }

    @PostMapping("/{id}/bloquear")
    public SolicitacaoDto bloquear(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) BloquearRequest body,
            Authentication authentication
    ) {
        String usuario = authentication != null ? authentication.getName() : "sistema";
        String motivo = body != null ? body.motivo() : null;
        solicitacaoAgendamentoService.bloquearPeriodoDaSolicitacao(id, motivo, usuario);
        return toDto(solicitacaoAgendamentoService.buscarPorId(id));
    }

    @PostMapping("/{id}/cancelar")
    public SolicitacaoDto cancelar(@PathVariable UUID id) {
        solicitacaoAgendamentoService.cancelarSolicitacaoAprovada(id);
        return toDto(solicitacaoAgendamentoService.buscarPorId(id));
    }

    @PostMapping("/{id}/restaurar")
    public SolicitacaoDto restaurar(@PathVariable UUID id) {
        solicitacaoAgendamentoService.restaurarSolicitacao(id);
        return toDto(solicitacaoAgendamentoService.buscarPorId(id));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{id}")
    public org.springframework.http.ResponseEntity<Void> excluir(@PathVariable UUID id) {
        solicitacaoAgendamentoService.excluirSolicitacao(id);
        return org.springframework.http.ResponseEntity.noContent().build();
    }

    private SolicitacaoDto toDto(SolicitacaoAgendamentoEntity entity) {
        return new SolicitacaoDto(
                entity.getId(),
                entity.getNomePaciente(),
                entity.getTelefone(),
                entity.getEmail(),
                entity.getDataHoraInicio(),
                entity.getDataHoraFim(),
                entity.getStatus(),
                entity.getObservacoes(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public record SolicitacaoDto(
            UUID id,
            String nomePaciente,
            String telefone,
            String email,
            java.time.OffsetDateTime dataHoraInicio,
            java.time.OffsetDateTime dataHoraFim,
            SolicitacaoStatus status,
            String observacoes,
            java.time.OffsetDateTime createdAt,
            java.time.OffsetDateTime updatedAt
    ) {}

    public record AprovarRequest(UUID planoSessaoId) {}

    public record CadastrarPacienteRequest(
            String nomeCompleto,
            String telefone,
            @Email(message = "Informe um e-mail válido.")
            String email,
            String cpf,
            String cep,
            String rua,
            String numero,
            String bairro,
            String cidade,
            String estado
    ) {}

    public record ReagendarRequest(
            @NotNull(message = "Informe a data para reagendar.")
            LocalDate data,
            @NotBlank(message = "Informe o horário no formato HH:mm.")
            String horario,
            String motivo
    ) {}

    public record BloquearRequest(String motivo) {}

    public record EditarRequest(
            @NotBlank(message = "Informe o nome do paciente.")
            String nomePaciente,
            @NotBlank(message = "Informe o telefone do paciente.")
            String telefone,
            @Email(message = "Informe um e-mail válido.")
            String email,
            @NotNull(message = "Informe a data do atendimento.")
            LocalDate data,
            @NotBlank(message = "Informe o horário no formato HH:mm.")
            String horario,
            @NotBlank(message = "Informe o local de atendimento.")
            String localAtendimento,
            String observacoes,
            String motivo
    ) {}
}
