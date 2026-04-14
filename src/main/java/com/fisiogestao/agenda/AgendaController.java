package com.fisiogestao.agenda;

import com.fisiogestao.agenda.dto.SessaoEdicaoDto;
import com.fisiogestao.agenda.form.AgendaManualForm;
import com.fisiogestao.agenda.service.AgendaService;
import com.fisiogestao.pacientes.form.PacoteConsumoForm;
import com.fisiogestao.pacientes.service.PacoteSessaoService;
import com.fisiogestao.shared.web.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/agenda")
@RequiredArgsConstructor
public class AgendaController {

    private final AgendaService agendaService;
    private final PacoteSessaoService pacoteSessaoService;

    @GetMapping
    public String agenda(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data
    ) {
        return construirDestinoCalendario(dataOuHoje(data));
    }

    @PostMapping("/sessoes")
    public String criarSessao(
            @Valid @ModelAttribute("agendaManualForm") AgendaManualForm form,
            BindingResult bindingResult,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Revise os dados da sessão antes de salvar.");
            return construirDestinoCalendario(dataOuHoje(form.getData()));
        }

        try {
            agendaService.agendarSessaoManual(form, authentication != null ? authentication.getName() : "Equipe da clínica");
            redirectAttributes.addFlashAttribute("successMessage", "Sessão agendada com sucesso.");
            return construirDestinoCalendario(form.getData());
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return construirDestinoCalendario(dataOuHoje(form.getData()));
        }
    }

    @PostMapping("/pacotes")
    public String criarPacotePelaAgenda(
            @Valid @ModelAttribute("pacoteAgendaForm") PacoteConsumoForm form,
            BindingResult bindingResult,
            @RequestParam(required = false, name = "agendaData") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate agendaData,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Revise os dados do plano antes de salvar.");
            return construirDestinoCalendario(dataOuHoje(agendaData));
        }

        try {
            pacoteSessaoService.criarViaCrud(form);
            redirectAttributes.addFlashAttribute("successMessage", "Plano de tratamento criado e selecionado para o agendamento.");
            return construirDestinoCalendario(dataOuHoje(agendaData));
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return construirDestinoCalendario(dataOuHoje(agendaData));
        }
    }

    @PostMapping("/sessoes/{id}")
    public String editarSessao(
            @PathVariable UUID id,
            @Valid @ModelAttribute("sessaoEdicaoForm") AgendaManualForm form,
            BindingResult bindingResult,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Revise os dados da sessão antes de salvar.");
            return construirDestinoCalendario(dataOuHoje(form.getData()));
        }

        try {
            agendaService.atualizarSessaoManual(id, form, authentication != null ? authentication.getName() : "Equipe da clínica");
            redirectAttributes.addFlashAttribute("successMessage", "Sessão atualizada com sucesso.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return construirDestinoCalendario(dataOuHoje(form.getData()));
        }
        return construirDestinoCalendario(form.getData());
    }

    @PostMapping("/sessoes/{id}/cancelar")
    public String cancelarSessao(
            @PathVariable UUID id,
            @RequestParam(required = false) String observacoes,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            RedirectAttributes redirectAttributes
    ) {
        LocalDate retorno = dataOuHoje(data);
        try {
            agendaService.cancelarSessao(id, observacoes);
            redirectAttributes.addFlashAttribute("successMessage", "Sessão cancelada com sucesso.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return construirDestinoCalendario(retorno);
    }

    @PostMapping("/sessoes/{id}/reabrir")
    public String reverterRealizacao(
            @PathVariable UUID id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            RedirectAttributes redirectAttributes
    ) {
        LocalDate retorno = dataOuHoje(data);
        try {
            agendaService.reverterRealizacao(id);
            redirectAttributes.addFlashAttribute("successMessage", "Sessão reaberta como agendada.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return construirDestinoCalendario(retorno);
    }

    @PostMapping("/sessoes/{id}/restaurar")
    public String restaurarSessaoCancelada(
            @PathVariable UUID id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            RedirectAttributes redirectAttributes
    ) {
        LocalDate retorno = dataOuHoje(data);
        try {
            agendaService.reabrirSessaoCancelada(id);
            redirectAttributes.addFlashAttribute("successMessage", "Sessão voltou para pendente.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return construirDestinoCalendario(retorno);
    }

    @PostMapping("/sessoes/{id}/realizar")
    public String concluirSessao(
            @PathVariable java.util.UUID id,
            @RequestParam(required = false) String observacoes,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            RedirectAttributes redirectAttributes
    ) {
        LocalDate retorno = dataOuHoje(data);
        try {
            agendaService.marcarComoRealizada(id, observacoes);
            redirectAttributes.addFlashAttribute("successMessage", "Sessão marcada como realizada.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return construirDestinoCalendario(retorno);
    }

    @PostMapping("/sessoes/{id}/excluir")
    public String excluirSessao(
            @PathVariable UUID id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            RedirectAttributes redirectAttributes
    ) {
        LocalDate retorno = dataOuHoje(data);
        try {
            agendaService.excluirSessao(id);
            redirectAttributes.addFlashAttribute("successMessage", "Sessão excluída com sucesso.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return construirDestinoCalendario(retorno);
    }

    @GetMapping("/sessoes/{id}/json")
    @ResponseBody
    public ResponseEntity<?> buscarSessaoJson(@PathVariable UUID id) {
        try {
            SessaoEntity sessao = agendaService.buscarSessao(id);
            ZoneId zoneId = ZoneId.of(agendaService.obterConfiguracaoAtiva().getTimezoneId());
            ZonedDateTime inicio = sessao.getDataHoraInicio().atZoneSameInstant(zoneId);
            SessaoEdicaoDto dto = SessaoEdicaoDto.of(
                    sessao.getId(),
                    sessao.getCliente().getId(),
                    sessao.getCliente().getNomeCompleto(),
                    sessao.getPacoteSessao() != null ? sessao.getPacoteSessao().getId() : null,
                    sessao.getPacoteSessao() != null ? sessao.getPacoteSessao().getNomePacote() : null,
                    inicio.toLocalDate(),
                    inicio.toLocalTime(),
                    sessao.getTipoAtendimento(),
                    sessao.getObservacoes(),
                    sessao.getStatus().name()
            );
            return ResponseEntity.ok(dto);
        } catch (BusinessException ex) {
            Map<String, String> error = new HashMap<>();
            error.put("error", ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @PostMapping("/sessoes/{id}/json")
    @ResponseBody
    public ResponseEntity<?> atualizarSessaoJson(
            @PathVariable UUID id,
            @RequestParam UUID clienteId,
            @RequestParam(required = false) UUID pacoteSessaoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horario,
            @RequestParam(required = false) String tipoAtendimento,
            @RequestParam(required = false) String observacoes,
            Authentication authentication
    ) {
        try {
            AgendaManualForm form = new AgendaManualForm();
            form.setClienteId(clienteId);
            form.setPacoteSessaoId(pacoteSessaoId);
            form.setData(data);
            form.setHorario(horario);
            form.setTipoAtendimento(tipoAtendimento);
            form.setObservacoes(observacoes);
            agendaService.atualizarSessaoManual(id, form, authentication != null ? authentication.getName() : "Equipe da clínica");
            Map<String, Object> success = new HashMap<>();
            success.put("success", true);
            success.put("message", "Sessão atualizada com sucesso.");
            return ResponseEntity.ok(success);
        } catch (BusinessException ex) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", ex.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    private String construirDestinoCalendario(LocalDate data) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/calendario");
        if (data != null) {
            builder.queryParam("data", data);
        }
        return "redirect:" + builder.build().encode().toUriString();
    }

    private LocalDate dataOuHoje(LocalDate data) {
        return data != null ? data : LocalDate.now();
    }
}
