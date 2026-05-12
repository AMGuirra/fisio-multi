package com.fisiogestao.agenda;

import com.fisiogestao.agenda.dto.SlotDisponivelDto;
import com.fisiogestao.agenda.service.AgendaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/slots")
@RequiredArgsConstructor
@Tag(name = "Slots Disponíveis", description = "Consulta pública de horários livres para agendamento online")
public class SlotsApiController {

    private final AgendaService agendaService;

    @GetMapping
    @Operation(
            summary = "Listar horários disponíveis em uma data",
            description = """
                    Retorna os slots de atendimento disponíveis para a data informada,
                    respeitando a configuração de horários da clínica e os agendamentos já existentes.

                    **Rota pública** — não requer autenticação. Usada pelo formulário de agendamento online.
                    """,
            parameters = {
                    @Parameter(name = "data", description = "Data no formato ISO 8601 (YYYY-MM-DD)", example = "2026-01-20", required = true)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lista de slots disponíveis",
                            content = @Content(mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = SlotDisponivelDto.class))))
            }
    )
    public ResponseEntity<List<SlotDisponivelDto>> slots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data
    ) {
        return ResponseEntity.ok(agendaService.gerarSlotsDisponiveis(data, true, 10));
    }
}
