package com.fisiogestao.agenda;

import com.fisiogestao.agenda.dto.AgendamentoRecorrenteRequestDto;
import com.fisiogestao.agenda.dto.AgendamentoRequestDto;
import com.fisiogestao.agenda.form.AgendaManualForm;
import com.fisiogestao.agenda.service.AgendaService;
import com.fisiogestao.shared.web.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/agendamentos")
@RequiredArgsConstructor
@PreAuthorize("@menuAccessSecurity.canAccess('AGENDA')")
@Tag(name = "Agendamentos", description = "Criação de sessões avulsas e séries recorrentes na agenda")
@SecurityRequirement(name = "basicAuth")
public class AgendamentosApiController {

    private final AgendaService agendaService;

    @PostMapping
    @Operation(
            summary = "Criar agendamento avulso",
            description = """
                    Cria uma sessão única na agenda para o paciente indicado.
                    O campo `tipo` deve ser `AVULSO` ou `PLANO`.
                    Quando `tipo = PLANO`, o campo `planoId` é obrigatório.
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Agendamento criado com sucesso",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(example = "{\"success\":true,\"id\":\"uuid\",\"message\":\"Agendamento criado.\"}"))),
                    @ApiResponse(responseCode = "400", description = "Dados inválidos ou conflito de horário",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(example = "{\"success\":false,\"error\":\"Mensagem de erro.\"}"))),
                    @ApiResponse(responseCode = "401", description = "Não autenticado")
            }
    )
    public ResponseEntity<?> criar(
            @Valid @RequestBody AgendamentoRequestDto request,
            Authentication authentication
    ) {
        return criarComum(request, authentication);
    }

    @PostMapping("/recorrentes")
    @Operation(
            summary = "Criar série de agendamentos recorrentes",
            description = """
                    Cria múltiplas sessões com base nas configurações de recorrência informadas
                    (semanal, quinzenal ou mensal, com data de término).
                    Retorna o número de sessões criadas.
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Série recorrente criada com sucesso",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(example = "{\"success\":true,\"id\":\"uuid\",\"message\":\"Agendamentos recorrentes criados.\"}"))),
                    @ApiResponse(responseCode = "400", description = "Dados inválidos ou recorrência sem sessões geradas"),
                    @ApiResponse(responseCode = "401", description = "Não autenticado")
            }
    )
    public ResponseEntity<?> criarRecorrente(
            @Valid @RequestBody AgendamentoRecorrenteRequestDto request,
            Authentication authentication
    ) {
        return criarComum(request, authentication);
    }

    private ResponseEntity<?> criarComum(AgendamentoRequestDto request, Authentication authentication) {
        try {
            var resultado = agendaService.agendarSessaoManual(paraForm(request), usuario(authentication));
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "id", resultado.getId(),
                    "message", request.recorrente() ? "Agendamentos recorrentes criados." : "Agendamento criado."
            ));
        } catch (BusinessException ex) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", ex.getMessage()));
        }
    }

    private AgendaManualForm paraForm(AgendamentoRequestDto request) {
        LocalDateTime inicio = request.getDataHoraInicio();
        AgendaManualForm form = new AgendaManualForm();
        form.setClienteId(request.getPacienteId());
        form.setTipoAgendamento(request.getTipo());
        form.setPlanoSessaoId(request.getPlanoId());
        form.setValorAvulso(request.getValorAvulso());
        form.setData(inicio.toLocalDate());
        form.setHorario(inicio.toLocalTime());
        form.setDuracaoMinutos(request.getDuracaoMinutos());
        form.setTipoAtendimento(request.getTipoAtendimento());
        form.setObservacoes(request.getObservacao());
        form.setRecorrente(request.recorrente());
        if (request instanceof AgendamentoRecorrenteRequestDto recorrente) {
            form.setRecorrencia(recorrente.getRecorrencia());
        }
        return form;
    }

    private String usuario(Authentication authentication) {
        return authentication != null ? authentication.getName() : "Equipe da clínica";
    }
}
