package com.fisiogestao.auditoria;

import com.fisiogestao.auditoria.form.AuditFilterForm;
import com.fisiogestao.auditoria.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.UUID;

@Controller
@RequestMapping("/auditoria")
@RequiredArgsConstructor
@SuppressWarnings("null")
public class AuditoriaController {

    private final AuditService auditService;

    @GetMapping
    public String index(@ModelAttribute("filtroForm") AuditFilterForm filtroForm, Model model) {
        AuditFilterForm filtro = filtroPadrao(filtroForm);
        var eventos = auditService.list(filtro);

        model.addAttribute("pageTitle", "Auditoria do Sistema");
        model.addAttribute("filtroForm", filtro);
        model.addAttribute("eventos", eventos);
        model.addAttribute("resumoAuditoria", auditService.summarize(eventos));
        model.addAttribute("modulosAuditoria", AuditModule.values());
        model.addAttribute("acoesAuditoria", AuditActionType.values());
        model.addAttribute("origensAuditoria", AuditEventOrigin.values());
        model.addAttribute("entidadesAuditoria", auditService.availableEntities());
        model.addAttribute("usuariosAuditoria", auditService.availableUsers());
        return "auditoria/index";
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable UUID id, Model model) {
        var evento = auditService.detail(id);
        model.addAttribute("pageTitle", "Detalhe da Auditoria");
        model.addAttribute("eventoAuditoria", evento);
        return "auditoria/detalhe";
    }

    @GetMapping("/exportar")
    public ResponseEntity<ByteArrayResource> exportar(
            @ModelAttribute("filtroForm") AuditFilterForm filtroForm,
            @RequestParam AuditService.AuditExportFormat formato
    ) {
        AuditFilterForm filtro = filtroPadrao(filtroForm);
        var eventos = auditService.list(filtro);
        AuditService.AuditDocument documento = auditService.export(eventos, formato);

        auditService.recordManual(
                AuditModule.AUDITORIA,
                "auditoria_sistema",
                null,
                "Central de Auditoria",
                formato == AuditService.AuditExportFormat.PDF ? AuditActionType.EXPORT_PDF : AuditActionType.EXPORT_EXCEL,
                null,
                payloadExportacao(filtro, formato, eventos.size()),
                null,
                "Exportação da Central de Auditoria.",
                AuditEventOrigin.MANUAL
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + documento.fileName() + "\"")
                .contentType(MediaType.parseMediaType(documento.contentType()))
                .contentLength(documento.content().length)
                .body(new ByteArrayResource(documento.content()));
    }

    private AuditFilterForm filtroPadrao(AuditFilterForm filtroForm) {
        if (filtroForm.getDataInicial() == null) {
            filtroForm.setDataInicial(LocalDate.now().minusDays(30));
        }
        if (filtroForm.getDataFinal() == null) {
            filtroForm.setDataFinal(LocalDate.now());
        }
        return filtroForm;
    }

    private LinkedHashMap<String, Object> payloadExportacao(
            AuditFilterForm filtro,
            AuditService.AuditExportFormat formato,
            int totalEventos
    ) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("formato", formato != null ? formato.name() : null);
        payload.put("dataInicial", filtro.getDataInicial());
        payload.put("dataFinal", filtro.getDataFinal());
        payload.put("modulo", filtro.getModulo() != null ? filtro.getModulo().name() : null);
        payload.put("entidade", filtro.getEntidade());
        payload.put("tipoAcao", filtro.getTipoAcao() != null ? filtro.getTipoAcao().name() : null);
        payload.put("usuario", filtro.getUsuario());
        payload.put("paciente", filtro.getPaciente());
        payload.put("profissional", filtro.getProfissional());
        payload.put("registroAfetado", filtro.getRegistroAfetado());
        payload.put("origemEvento", filtro.getOrigemEvento() != null ? filtro.getOrigemEvento().name() : null);
        payload.put("somenteExclusoes", filtro.isSomenteExclusoes());
        payload.put("somenteCancelamentos", filtro.isSomenteCancelamentos());
        payload.put("somenteEdicoes", filtro.isSomenteEdicoes());
        payload.put("somenteAlteracoesCriticas", filtro.isSomenteAlteracoesCriticas());
        payload.put("totalEventos", totalEventos);
        return payload;
    }
}
