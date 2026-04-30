package com.fisiogestao.auditoria.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fisiogestao.agenda.repository.ConfiguracaoClinicaRepository;
import com.fisiogestao.auditoria.AuditActionType;
import com.fisiogestao.auditoria.AuditActor;
import com.fisiogestao.auditoria.AuditEventCommand;
import com.fisiogestao.auditoria.AuditEventEntity;
import com.fisiogestao.auditoria.AuditEventOrigin;
import com.fisiogestao.auditoria.AuditFieldChangeEntity;
import com.fisiogestao.auditoria.AuditModule;
import com.fisiogestao.auditoria.form.AuditFilterForm;
import com.fisiogestao.auditoria.repository.AuditEventRepository;
import com.fisiogestao.auth.UsuarioSistemaRepository;
import com.fisiogestao.config.ClinicTimezoneHolder;
import com.fisiogestao.shared.web.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private static final List<AuditActionType> DELETE_ACTIONS = List.of(
            AuditActionType.DELETE_LOGICAL,
            AuditActionType.DELETE_PHYSICAL
    );

    private static final List<AuditActionType> CANCEL_ACTIONS = List.of(
            AuditActionType.CANCEL,
            AuditActionType.REJECT,
            AuditActionType.NO_SHOW
    );

    private final AuditEventRepository auditEventRepository;
    private final AuditSnapshotFactory auditSnapshotFactory;
    private final UsuarioSistemaRepository usuarioSistemaRepository;
    private final ConfiguracaoClinicaRepository configuracaoClinicaRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public AuditEventEntity record(AuditEventCommand command) {
        if (command == null) {
            throw new BusinessException("Evento de auditoria inválido.");
        }

        Map<String, Object> oldPayload = normalizePayload(command.oldPayload());
        Map<String, Object> newPayload = normalizePayload(command.newPayload());
        List<AuditFieldChangeEntity> diffs = buildDiffs(oldPayload, newPayload);

        AuditActor actor = resolveActor(command.actor());
        RequestInfo requestInfo = resolveRequestInfo(command);
        String unidadeId = command.unitId() != null ? command.unitId() : resolveClinicUnitId();

        AuditEventEntity entity = AuditEventEntity.builder()
                .modulo(command.module())
                .entidade(text(command.entity()))
                .entidadeId(text(command.entityId()))
                .descricaoRegistro(text(command.recordDescription()))
                .tipoAcao(command.actionType())
                .campoAlterado(summaryFieldName(diffs))
                .valorAnterior(summaryOldValue(diffs))
                .valorNovo(summaryNewValue(diffs))
                .payloadAnteriorJson(toJson(oldPayload))
                .payloadNovoJson(toJson(newPayload))
                .motivo(text(command.reason()))
                .observacao(text(command.observation()))
                .usuarioId(actor != null ? actor.userId() : null)
                .usuarioNome(actor != null ? actor.userName() : null)
                .perfilUsuario(actor != null ? actor.userRole() : null)
                .origemEvento(command.origin() != null ? command.origin() : AuditEventOrigin.MANUAL)
                .ipOrigem(requestInfo.ipAddress())
                .userAgent(requestInfo.userAgent())
                .unidadeId(unidadeId)
                .sucessoOperacao(command.success() == null || command.success())
                .alteracaoCritica(command.critical() != null ? command.critical() : inferCritical(command.actionType(), diffs))
                .build();

        diffs.forEach(entity::addFieldChange);
        return auditEventRepository.save(entity);
    }

    public AuditEventEntity recordCreate(
            AuditModule module,
            String entity,
            Object newState,
            String reason,
            String observation,
            AuditEventOrigin origin
    ) {
        return record(AuditEventCommand.builder()
                .module(module)
                .entity(entity)
                .entityId(extractEntityId(newState))
                .recordDescription(auditSnapshotFactory.describe(newState))
                .actionType(AuditActionType.CREATE)
                .newPayload(auditSnapshotFactory.snapshot(newState))
                .reason(reason)
                .observation(observation)
                .origin(origin)
                .build());
    }

    public Map<String, Object> snapshotOf(Object source) {
        return auditSnapshotFactory.snapshot(source);
    }

    public String describeOf(Object source) {
        return auditSnapshotFactory.describe(source);
    }

    public AuditEventEntity recordUpdate(
            AuditModule module,
            String entity,
            String entityId,
            String description,
            Object oldState,
            Object newState,
            String reason,
            String observation,
            AuditEventOrigin origin
    ) {
        return record(AuditEventCommand.builder()
                .module(module)
                .entity(entity)
                .entityId(entityId != null ? entityId : extractEntityId(newState))
                .recordDescription(description != null ? description : auditSnapshotFactory.describe(newState))
                .actionType(AuditActionType.UPDATE)
                .oldPayload(auditSnapshotFactory.snapshot(oldState))
                .newPayload(auditSnapshotFactory.snapshot(newState))
                .reason(reason)
                .observation(observation)
                .origin(origin)
                .build());
    }

    public AuditEventEntity recordAction(
            AuditModule module,
            String entity,
            String entityId,
            String description,
            AuditActionType actionType,
            Object oldState,
            Object newState,
            String reason,
            String observation,
            AuditEventOrigin origin
    ) {
        return record(AuditEventCommand.builder()
                .module(module)
                .entity(entity)
                .entityId(entityId)
                .recordDescription(description)
                .actionType(actionType)
                .oldPayload(auditSnapshotFactory.snapshot(oldState))
                .newPayload(auditSnapshotFactory.snapshot(newState))
                .reason(reason)
                .observation(observation)
                .origin(origin)
                .build());
    }

    public AuditEventEntity recordManual(
            AuditModule module,
            String entity,
            String entityId,
            String description,
            AuditActionType actionType,
            Map<String, Object> oldPayload,
            Map<String, Object> newPayload,
            String reason,
            String observation,
            AuditEventOrigin origin
    ) {
        return record(AuditEventCommand.builder()
                .module(module)
                .entity(entity)
                .entityId(entityId)
                .recordDescription(description)
                .actionType(actionType)
                .oldPayload(oldPayload)
                .newPayload(newPayload)
                .reason(reason)
                .observation(observation)
                .origin(origin)
                .build());
    }

    public List<AuditEventEntity> list(AuditFilterForm filter) {
        return auditEventRepository.findAll(buildSpecification(filter), Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public AuditEventEntity detail(UUID id) {
        return auditEventRepository.findWithFieldChangesById(id)
                .orElseThrow(() -> new BusinessException("Registro de auditoria não encontrado."));
    }

    public AuditSummary summarize(List<AuditEventEntity> events) {
        long total = events.size();
        long inclusoes = countByAction(events, AuditActionType.CREATE);
        long edicoes = countByAction(events, AuditActionType.UPDATE, AuditActionType.RESCHEDULE, AuditActionType.MARK_AS_PAID);
        long exclusoes = countByAction(events, AuditActionType.DELETE_LOGICAL, AuditActionType.DELETE_PHYSICAL);
        long cancelamentos = countByAction(events, AuditActionType.CANCEL, AuditActionType.REJECT, AuditActionType.NO_SHOW);
        long confirmacoes = countByAction(events, AuditActionType.CONFIRM, AuditActionType.COMPLETE, AuditActionType.ACTIVATE);
        long exportacoes = countByAction(events, AuditActionType.EXPORT_PDF, AuditActionType.EXPORT_EXCEL);
        long envios = countByAction(events, AuditActionType.SEND_EMAIL);
        return new AuditSummary(total, inclusoes, edicoes, exclusoes, cancelamentos, confirmacoes, exportacoes, envios);
    }

    public List<String> availableEntities() {
        return auditEventRepository.findAll(Sort.by("entidade")).stream()
                .map(AuditEventEntity::getEntidade)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
    }

    public List<String> availableUsers() {
        return auditEventRepository.findAll(Sort.by("usuarioNome")).stream()
                .map(AuditEventEntity::getUsuarioNome)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
    }

    public AuditDocument export(List<AuditEventEntity> events, AuditExportFormat format) {
        return switch (format) {
            case PDF -> generatePdf(events);
            case EXCEL -> generateExcel(events);
        };
    }

    private Specification<AuditEventEntity> buildSpecification(AuditFilterForm filter) {
        AuditFilterForm effectiveFilter = filter != null ? filter : new AuditFilterForm();
        LocalDate dataInicial = effectiveFilter.getDataInicial();
        LocalDate dataFinal = effectiveFilter.getDataFinal();

        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (dataInicial != null) {
                OffsetDateTime inicio = dataInicial.atStartOfDay(resolveAppZone()).toOffsetDateTime();
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), inicio));
            }
            if (dataFinal != null) {
                OffsetDateTime fim = dataFinal.plusDays(1).atStartOfDay(resolveAppZone()).toOffsetDateTime();
                predicates.add(cb.lessThan(root.get("createdAt"), fim));
            }
            if (effectiveFilter.getModulo() != null) {
                predicates.add(cb.equal(root.get("modulo"), effectiveFilter.getModulo()));
            }
            if (effectiveFilter.getTipoAcao() != null) {
                predicates.add(cb.equal(root.get("tipoAcao"), effectiveFilter.getTipoAcao()));
            }
            if (hasText(effectiveFilter.getEntidade())) {
                predicates.add(cb.equal(cb.lower(root.get("entidade")), effectiveFilter.getEntidade().trim().toLowerCase()));
            }
            if (hasText(effectiveFilter.getUsuario())) {
                String pattern = "%" + effectiveFilter.getUsuario().trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("usuarioNome")), pattern));
            }
            if (hasText(effectiveFilter.getRegistroAfetado())) {
                String pattern = "%" + effectiveFilter.getRegistroAfetado().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("entidadeId")), pattern),
                        cb.like(cb.lower(root.get("descricaoRegistro")), pattern)
                ));
            }
            if (hasText(effectiveFilter.getPaciente())) {
                String pattern = "%" + effectiveFilter.getPaciente().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("descricaoRegistro")), pattern),
                        cb.like(cb.lower(root.get("payloadAnteriorJson")), pattern),
                        cb.like(cb.lower(root.get("payloadNovoJson")), pattern)
                ));
            }
            if (hasText(effectiveFilter.getProfissional())) {
                String pattern = "%" + effectiveFilter.getProfissional().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("descricaoRegistro")), pattern),
                        cb.like(cb.lower(root.get("payloadAnteriorJson")), pattern),
                        cb.like(cb.lower(root.get("payloadNovoJson")), pattern),
                        cb.like(cb.lower(root.get("observacao")), pattern)
                ));
            }
            if (effectiveFilter.getOrigemEvento() != null) {
                predicates.add(cb.equal(root.get("origemEvento"), effectiveFilter.getOrigemEvento()));
            }
            if (effectiveFilter.isSomenteExclusoes()) {
                predicates.add(root.get("tipoAcao").in(DELETE_ACTIONS));
            }
            if (effectiveFilter.isSomenteCancelamentos()) {
                predicates.add(root.get("tipoAcao").in(CANCEL_ACTIONS));
            }
            if (effectiveFilter.isSomenteEdicoes()) {
                predicates.add(root.get("tipoAcao").in(List.of(
                        AuditActionType.UPDATE,
                        AuditActionType.RESCHEDULE,
                        AuditActionType.MARK_AS_PAID
                )));
            }
            if (effectiveFilter.isSomenteAlteracoesCriticas()) {
                predicates.add(cb.isTrue(root.get("alteracaoCritica")));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private AuditDocument generatePdf(List<AuditEventEntity> events) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            com.lowagie.text.Document document = new com.lowagie.text.Document();
            com.lowagie.text.pdf.PdfWriter.getInstance(document, output);
            document.open();
            document.add(new com.lowagie.text.Paragraph(
                    "Auditoria do Sistema",
                    com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 16)
            ));
            document.add(new com.lowagie.text.Paragraph(
                    "Gerado em " + OffsetDateTime.now(resolveAppZone()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            ));
            document.add(new com.lowagie.text.Paragraph(" "));

            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(7);
            table.setWidthPercentage(100);
            addHeader(table, "Data/Hora");
            addHeader(table, "Módulo");
            addHeader(table, "Entidade");
            addHeader(table, "Ação");
            addHeader(table, "Registro");
            addHeader(table, "Usuário");
            addHeader(table, "Origem");

            for (AuditEventEntity event : events) {
                addValue(table, formatDateTime(event.getCreatedAt()));
                addValue(table, event.getModulo() != null ? event.getModulo().getLabel() : "-");
                addValue(table, text(event.getEntidade(), "-"));
                addValue(table, event.getTipoAcao() != null ? event.getTipoAcao().getLabel() : "-");
                addValue(table, text(event.getDescricaoRegistro(), "-"));
                addValue(table, text(event.getUsuarioNome(), "-"));
                addValue(table, event.getOrigemEvento() != null ? event.getOrigemEvento().getLabel() : "-");
            }

            document.add(table);
            document.close();
            return new AuditDocument("auditoria-sistema.pdf", "application/pdf", output.toByteArray());
        } catch (Exception ex) {
            throw new BusinessException("Não foi possível gerar o PDF da auditoria.");
        }
    }

    private AuditDocument generateExcel(List<AuditEventEntity> events) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Auditoria");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Data/Hora");
            header.createCell(1).setCellValue("Módulo");
            header.createCell(2).setCellValue("Entidade");
            header.createCell(3).setCellValue("Ação");
            header.createCell(4).setCellValue("Registro");
            header.createCell(5).setCellValue("Usuário");
            header.createCell(6).setCellValue("Origem");
            header.createCell(7).setCellValue("Motivo");
            header.createCell(8).setCellValue("Observação");

            int rowIndex = 1;
            for (AuditEventEntity event : events) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(formatDateTime(event.getCreatedAt()));
                row.createCell(1).setCellValue(event.getModulo() != null ? event.getModulo().getLabel() : "");
                row.createCell(2).setCellValue(text(event.getEntidade(), ""));
                row.createCell(3).setCellValue(event.getTipoAcao() != null ? event.getTipoAcao().getLabel() : "");
                row.createCell(4).setCellValue(text(event.getDescricaoRegistro(), ""));
                row.createCell(5).setCellValue(text(event.getUsuarioNome(), ""));
                row.createCell(6).setCellValue(event.getOrigemEvento() != null ? event.getOrigemEvento().getLabel() : "");
                row.createCell(7).setCellValue(text(event.getMotivo(), ""));
                row.createCell(8).setCellValue(text(event.getObservacao(), ""));
            }

            workbook.write(output);
            return new AuditDocument(
                    "auditoria-sistema.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    output.toByteArray()
            );
        } catch (Exception ex) {
            throw new BusinessException("Não foi possível gerar a planilha da auditoria.");
        }
    }

    private void addHeader(com.lowagie.text.pdf.PdfPTable table, String value) {
        table.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(
                value,
                com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 9)
        )));
    }

    private void addValue(com.lowagie.text.pdf.PdfPTable table, String value) {
        table.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(
                value,
                com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA, 8)
        )));
    }

    private AuditActor resolveActor(AuditActor actorOverride) {
        if (actorOverride != null) {
            return actorOverride;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        String username = authentication.getName();
        if (username == null || username.isBlank()) {
            return null;
        }
        return usuarioSistemaRepository.findByUsername(username.trim().toLowerCase())
                .map(AuditActor::fromUser)
                .orElse(AuditActor.builder().username(username).userName(username).build());
    }

    private RequestInfo resolveRequestInfo(AuditEventCommand command) {
        String ip = command.ipAddress();
        String userAgent = command.userAgent();

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return new RequestInfo(ip, truncate(userAgent, 500));
        }

        HttpServletRequest request = attributes.getRequest();
        String resolvedIp = hasText(ip) ? ip : extractClientIp(request);
        String resolvedUserAgent = hasText(userAgent) ? userAgent : request.getHeader("User-Agent");
        return new RequestInfo(truncate(resolvedIp, 64), truncate(resolvedUserAgent, 500));
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String resolveClinicUnitId() {
        return configuracaoClinicaRepository.findFirstByAtivoTrueOrderByCreatedAtAsc()
                .map(config -> config.getId().toString())
                .orElse(null);
    }

    private Map<String, Object> normalizePayload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        LinkedHashMap<String, Object> normalized = new LinkedHashMap<>();
        payload.forEach((key, value) -> normalized.put(key, normalizeValue(value)));
        return normalized;
    }

    private Object normalizeValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.setScale(2, RoundingMode.HALF_UP).toPlainString();
        }
        if (value instanceof UUID || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof OffsetDateTime || value instanceof LocalDate) {
            return value.toString();
        }
        return String.valueOf(value);
    }

    private List<AuditFieldChangeEntity> buildDiffs(Map<String, Object> oldPayload, Map<String, Object> newPayload) {
        if (oldPayload == null && newPayload == null) {
            return List.of();
        }
        Set<String> fields = new LinkedHashSet<>();
        if (oldPayload != null) {
            fields.addAll(oldPayload.keySet());
        }
        if (newPayload != null) {
            fields.addAll(newPayload.keySet());
        }

        ArrayList<AuditFieldChangeEntity> changes = new ArrayList<>();
        for (String field : fields) {
            Object oldValue = oldPayload != null ? oldPayload.get(field) : null;
            Object newValue = newPayload != null ? newPayload.get(field) : null;
            if (Objects.equals(oldValue, newValue)) {
                continue;
            }
            changes.add(AuditFieldChangeEntity.builder()
                    .fieldName(field)
                    .oldValue(stringify(oldValue))
                    .newValue(stringify(newValue))
                    .build());
        }
        return changes;
    }

    private boolean inferCritical(AuditActionType actionType, List<AuditFieldChangeEntity> diffs) {
        if (actionType != null && actionType.isCritical()) {
            return true;
        }
        return diffs.stream()
                .map(AuditFieldChangeEntity::getFieldName)
                .anyMatch(field -> List.of(
                        "status",
                        "role",
                        "valor",
                        "dataHoraInicio",
                        "dataHoraFim",
                        "pacoteConsumoRegistrado"
                ).contains(field));
    }

    private String summaryFieldName(List<AuditFieldChangeEntity> diffs) {
        if (diffs.isEmpty()) {
            return null;
        }
        if (diffs.size() == 1) {
            return diffs.getFirst().getFieldName();
        }
        return diffs.stream()
                .limit(4)
                .map(AuditFieldChangeEntity::getFieldName)
                .reduce((left, right) -> left + ", " + right)
                .orElse(null);
    }

    private String summaryOldValue(List<AuditFieldChangeEntity> diffs) {
        if (diffs.size() != 1) {
            return null;
        }
        return diffs.getFirst().getOldValue();
    }

    private String summaryNewValue(List<AuditFieldChangeEntity> diffs) {
        if (diffs.size() != 1) {
            return null;
        }
        return diffs.getFirst().getNewValue();
    }

    private String extractEntityId(Object source) {
        if (source == null) {
            return null;
        }
        Map<String, Object> payload = auditSnapshotFactory.snapshot(source);
        if (payload == null) {
            return null;
        }
        Object id = payload.get("id");
        return id != null ? String.valueOf(id) : null;
    }

    private String stringify(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String stringValue) {
            return stringValue;
        }
        return toJson(value);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return String.valueOf(value);
        }
    }

    private String formatDateTime(OffsetDateTime value) {
        if (value == null) {
            return "";
        }
        return value.atZoneSameInstant(resolveAppZone()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    private long countByAction(List<AuditEventEntity> events, AuditActionType... actionTypes) {
        List<AuditActionType> allowed = List.of(actionTypes);
        return events.stream()
                .filter(event -> allowed.contains(event.getTipoAcao()))
                .count();
    }

    private ZoneId resolveAppZone() {
        return configuracaoClinicaRepository.findFirstByAtivoTrueOrderByCreatedAtAsc()
                .map(config -> ZoneId.of(config.getTimezoneId()))
                .orElse(ClinicTimezoneHolder.zoneId());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String text(String value) {
        return text(value, null);
    }

    private String text(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    private record RequestInfo(String ipAddress, String userAgent) {
    }

    public enum AuditExportFormat {
        PDF,
        EXCEL
    }

    public record AuditSummary(
            long totalAcoes,
            long totalInclusoes,
            long totalEdicoes,
            long totalExclusoes,
            long totalCancelamentos,
            long totalConfirmacoes,
            long totalExportacoes,
            long totalEnviosEmail
    ) {
    }

    public record AuditDocument(String fileName, String contentType, byte[] content) {
    }
}
