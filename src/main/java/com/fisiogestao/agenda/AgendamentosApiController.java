package com.fisiogestao.agenda;

import com.fisiogestao.agenda.dto.AgendamentoRecorrenteRequestDto;
import com.fisiogestao.agenda.dto.AgendamentoRequestDto;
import com.fisiogestao.agenda.form.AgendaManualForm;
import com.fisiogestao.agenda.service.AgendaService;
import com.fisiogestao.shared.web.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
public class AgendamentosApiController {

    private final AgendaService agendaService;

    @PostMapping
    public ResponseEntity<?> criar(
            @Valid @RequestBody AgendamentoRequestDto request,
            Authentication authentication
    ) {
        return criarComum(request, authentication);
    }

    @PostMapping("/recorrentes")
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
        form.setPacoteSessaoId(request.getPacoteId());
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
