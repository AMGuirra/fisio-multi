package com.fisiogestao.agenda.service;

import com.fisiogestao.agenda.BloqueioAgendaEntity;
import com.fisiogestao.agenda.BloqueioAgendaOrigem;
import com.fisiogestao.agenda.BloqueioAgendaStatus;
import com.fisiogestao.agenda.ConfiguracaoClinicaEntity;
import com.fisiogestao.agenda.SessaoStatus;
import com.fisiogestao.agenda.form.BloqueioAgendaCadastroForm;
import com.fisiogestao.agenda.form.BloqueioAgendaFiltroForm;
import com.fisiogestao.agenda.repository.BloqueioAgendaRepository;
import com.fisiogestao.agenda.repository.ConfiguracaoClinicaRepository;
import com.fisiogestao.agenda.repository.SessaoRepository;
import com.fisiogestao.auditoria.AuditActionType;
import com.fisiogestao.auditoria.AuditEventOrigin;
import com.fisiogestao.auditoria.AuditModule;
import com.fisiogestao.auditoria.service.AuditService;
import com.fisiogestao.shared.web.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class BloqueioAgendaService {

    private static final List<SessaoStatus> STATUS_OCUPADOS = List.of(SessaoStatus.AGENDADA, SessaoStatus.REALIZADA);

    private final BloqueioAgendaRepository bloqueioAgendaRepository;
    private final ConfiguracaoClinicaRepository configuracaoClinicaRepository;
    private final SessaoRepository sessaoRepository;
    private final AuditService auditService;

    public List<BloqueioAgendaEntity> listarPorFiltro(BloqueioAgendaFiltroForm filtro) {
        LocalDate hoje = LocalDate.now();
        return bloqueioAgendaRepository.findAllByOrderByDataHoraInicioDesc().stream()
                .filter(item -> filtro.getDataInicial() == null || !item.getDataFinal().isBefore(filtro.getDataInicial()))
                .filter(item -> filtro.getDataFinal() == null || !item.getDataInicial().isAfter(filtro.getDataFinal()))
                .filter(item -> filtro.getMotivo() == null || filtro.getMotivo().isBlank() || item.getMotivo().toLowerCase().contains(filtro.getMotivo().trim().toLowerCase()))
                .filter(item -> !filtro.isApenasAtivos() || item.getStatus() == BloqueioAgendaStatus.ATIVO)
                .filter(item -> filtro.getStatus() == null || item.getStatus() == filtro.getStatus())
                .filter(item -> filtro.getTipo() == null || filtro.getTipo().isBlank()
                        || ("DIA_INTEIRO".equalsIgnoreCase(filtro.getTipo()) && Boolean.TRUE.equals(item.getDiaInteiro()))
                        || ("POR_HORARIO".equalsIgnoreCase(filtro.getTipo()) && Boolean.FALSE.equals(item.getDiaInteiro())))
                .filter(item -> filtrarPeriodo(item, filtro.getPeriodo(), hoje))
                .sorted(Comparator.comparing(BloqueioAgendaEntity::getDataHoraInicio).reversed())
                .toList();
    }

    public BloqueioEdicaoView carregarParaEdicao(UUID id) {
        BloqueioAgendaEntity base = buscarPorId(id);
        List<BloqueioAgendaEntity> grupo = grupo(base);
        BloqueioAgendaCadastroForm form = new BloqueioAgendaCadastroForm();
        form.setDataInicial(grupo.getFirst().getDataInicial());
        form.setDataFinal(grupo.getLast().getDataFinal());
        form.setBloqueioDiaInteiro(Boolean.TRUE.equals(base.getDiaInteiro()));
        form.setHoraInicio(Boolean.TRUE.equals(base.getDiaInteiro()) ? null : grupo.getFirst().getHoraInicio());
        form.setHoraFim(Boolean.TRUE.equals(base.getDiaInteiro()) ? null : grupo.getFirst().getHoraFim());
        form.setMotivo(base.getMotivo());
        form.setObservacao(base.getObservacao());
        return new BloqueioEdicaoView(base, form, grupo);
    }

    public List<BloqueioAgendaEntity> listarAtivosNoPeriodo(LocalDate inicioInclusive, LocalDate fimExclusive) {
        ZoneId zoneId = zoneId();
        return bloqueioAgendaRepository.findByStatusAndDataHoraInicioLessThanAndDataHoraFimGreaterThan(
                BloqueioAgendaStatus.ATIVO,
                fimExclusive.atStartOfDay(zoneId).toOffsetDateTime(),
                inicioInclusive.atStartOfDay(zoneId).toOffsetDateTime()
        );
    }

    public List<BloqueioAgendaEntity> listarAtivosDoDia(LocalDate data) {
        ZoneId zoneId = zoneId();
        OffsetDateTime inicio = data.atStartOfDay(zoneId).toOffsetDateTime();
        return bloqueioAgendaRepository.findByStatusAndDataHoraInicioLessThanAndDataHoraFimGreaterThan(
                BloqueioAgendaStatus.ATIVO,
                inicio.plusDays(1),
                inicio
        );
    }

    public boolean existeBloqueioAtivo(OffsetDateTime inicio, OffsetDateTime fim, UUID ignorarId) {
        return listarConflitos(inicio, fim, ignorarId).stream().findAny().isPresent();
    }

    public List<BloqueioAgendaEntity> listarConflitos(OffsetDateTime inicio, OffsetDateTime fim, UUID ignorarId) {
        return bloqueioAgendaRepository.findByStatusAndDataHoraInicioLessThanAndDataHoraFimGreaterThan(
                        BloqueioAgendaStatus.ATIVO, fim, inicio)
                .stream()
                .filter(item -> ignorarId == null || !item.getId().equals(ignorarId))
                .toList();
    }

    @Transactional
    public BloqueioAgendaEntity criar(BloqueioAgendaCadastroForm form, String usuario) {
        return criar(form, usuario, BloqueioAgendaOrigem.MANUAL);
    }

    @Transactional
    public BloqueioAgendaEntity criar(BloqueioAgendaCadastroForm form, String usuario, BloqueioAgendaOrigem origem) {
        List<BloqueioAgendaEntity> criados = salvarLote(null, form, usuario, origem);
        auditService.recordManual(
                AuditModule.BLOQUEIOS,
                "bloqueio_agenda",
                resolveAuditEntityId(criados),
                describeAuditRecord(criados),
                AuditActionType.CREATE,
                null,
                snapshotGrupo(criados),
                form.getMotivo(),
                "Bloqueio cadastrado na agenda.",
                mapAuditOrigin(origem)
        );
        return criados.getFirst();
    }

    @Transactional
    public BloqueioAgendaEntity atualizar(UUID id, BloqueioAgendaCadastroForm form, String usuario) {
        BloqueioAgendaEntity existente = buscarPorId(id);
        validarEdicaoPermitida(existente);
        List<BloqueioAgendaEntity> grupoAnterior = grupo(existente);
        LinkedHashMap<String, Object> snapshotAnterior = snapshotGrupo(grupoAnterior);
        grupoAnterior.forEach(item -> {
            item.setStatus(BloqueioAgendaStatus.CANCELADO);
            item.setAtivo(false);
            item.setAtualizadoPor(usuario);
            bloqueioAgendaRepository.save(item);
        });
        List<BloqueioAgendaEntity> atualizados = salvarLote(
                existente.getGrupoReferencia() != null ? existente.getGrupoReferencia() : UUID.randomUUID(),
                form,
                usuario,
                existente.getOrigem() != null ? existente.getOrigem() : BloqueioAgendaOrigem.MANUAL
        );
        auditService.recordManual(
                AuditModule.BLOQUEIOS,
                "bloqueio_agenda",
                resolveAuditEntityId(atualizados),
                describeAuditRecord(atualizados),
                AuditActionType.UPDATE,
                snapshotAnterior,
                snapshotGrupo(atualizados),
                form.getMotivo(),
                "Bloqueio atualizado na agenda.",
                mapAuditOrigin(existente.getOrigem())
        );
        return atualizados.getFirst();
    }

    @Transactional
    public void cancelar(UUID id, String usuario) {
        BloqueioAgendaEntity existente = buscarPorId(id);
        validarEdicaoPermitida(existente);
        List<BloqueioAgendaEntity> grupo = grupo(existente);
        LinkedHashMap<String, Object> snapshotAnterior = snapshotGrupo(grupo);
        grupo.forEach(item -> {
            item.setStatus(BloqueioAgendaStatus.CANCELADO);
            item.setAtivo(false);
            item.setAtualizadoPor(usuario);
            bloqueioAgendaRepository.save(item);
        });
        auditService.recordManual(
                AuditModule.BLOQUEIOS,
                "bloqueio_agenda",
                resolveAuditEntityId(grupo),
                describeAuditRecord(grupo),
                AuditActionType.CANCEL,
                snapshotAnterior,
                snapshotGrupo(grupo),
                existente.getMotivo(),
                "Bloqueio cancelado na agenda.",
                mapAuditOrigin(existente.getOrigem())
        );
    }

    public ResumoBloqueio resumo(BloqueioAgendaFiltroForm filtro) {
        List<BloqueioAgendaEntity> lista = listarPorFiltro(filtro);
        long ativos = lista.stream().filter(item -> item.getStatus() == BloqueioAgendaStatus.ATIVO).count();
        long diaInteiro = lista.stream().filter(item -> Boolean.TRUE.equals(item.getDiaInteiro())).count();
        return new ResumoBloqueio(lista.size(), ativos, diaInteiro);
    }

    private List<BloqueioAgendaEntity> salvarLote(
            UUID grupoReferenciaExistente,
            BloqueioAgendaCadastroForm form,
            String usuario,
            BloqueioAgendaOrigem origem
    ) {
        ConfiguracaoClinicaEntity configuracao = configuracao();
        validarFormulario(form);

        UUID grupoReferencia = form.getDataInicial().equals(form.getDataFinal()) ? null
                : (grupoReferenciaExistente != null ? grupoReferenciaExistente : UUID.randomUUID());

        ArrayList<BloqueioAgendaEntity> criados = new ArrayList<>();
        LocalDate cursor = form.getDataInicial();
        while (!cursor.isAfter(form.getDataFinal())) {
            Intervalo intervalo = montarIntervalo(form, cursor, configuracao);
            validarConflitos(intervalo.inicio(), intervalo.fim(), null);

            BloqueioAgendaEntity entity = BloqueioAgendaEntity.builder()
                    .dataHoraInicio(intervalo.inicio())
                    .dataHoraFim(intervalo.fim())
                    .diaInteiro(form.isBloqueioDiaInteiro())
                    .motivo(form.getMotivo().trim())
                    .observacao(texto(form.getObservacao()))
                    .status(BloqueioAgendaStatus.ATIVO)
                    .origem(origem != null ? origem : BloqueioAgendaOrigem.MANUAL)
                    .criadoPor(usuario)
                    .atualizadoPor(usuario)
                    .ativo(true)
                    .grupoReferencia(grupoReferencia)
                    .build();
            criados.add(bloqueioAgendaRepository.save(entity));
            cursor = cursor.plusDays(1);
        }
        return criados;
    }

    private void validarFormulario(BloqueioAgendaCadastroForm form) {
        if (form.getDataInicial() == null || form.getDataFinal() == null) {
            throw new BusinessException("Informe a data inicial e final do bloqueio.");
        }
        if (form.getDataFinal().isBefore(form.getDataInicial())) {
            throw new BusinessException("A data final não pode ser anterior à data inicial.");
        }
        if (form.getMotivo() == null || form.getMotivo().isBlank()) {
            throw new BusinessException("Informe o motivo do bloqueio.");
        }
        if (!form.isBloqueioDiaInteiro()) {
            if (form.getHoraInicio() == null || form.getHoraFim() == null) {
                throw new BusinessException("Informe hora inicial e final para bloqueios por horário.");
            }
            if (!form.getHoraFim().isAfter(form.getHoraInicio())) {
                throw new BusinessException("A hora final deve ser maior que a hora inicial.");
            }
        }
    }

    private Intervalo montarIntervalo(BloqueioAgendaCadastroForm form, LocalDate data, ConfiguracaoClinicaEntity configuracao) {
        ZoneId zoneId = ZoneId.of(configuracao.getTimezoneId());
        LocalTime inicio = form.isBloqueioDiaInteiro() ? configuracao.getHorarioAbertura() : form.getHoraInicio();
        LocalTime fim = form.isBloqueioDiaInteiro() ? configuracao.getHorarioFechamento() : form.getHoraFim();
        return new Intervalo(data.atTime(inicio).atZone(zoneId).toOffsetDateTime(), data.atTime(fim).atZone(zoneId).toOffsetDateTime());
    }

    private void validarConflitos(OffsetDateTime inicio, OffsetDateTime fim, UUID ignorarId) {
        if (!inicio.isBefore(fim)) {
            throw new BusinessException("O período do bloqueio é inválido.");
        }
        if (!listarConflitos(inicio, fim, ignorarId).isEmpty()) {
            throw new BusinessException("Já existe um bloqueio ativo nesse período.");
        }
        if (sessaoRepository.existsByStatusInAndDataHoraInicioLessThanAndDataHoraFimGreaterThan(STATUS_OCUPADOS, fim, inicio)) {
            throw new BusinessException("Existe agendamento ativo no período informado. Ajuste ou remova os agendamentos antes de bloquear.");
        }
    }

    private boolean filtrarPeriodo(BloqueioAgendaEntity item, String periodo, LocalDate hoje) {
        String periodoNormalizado = periodo == null || periodo.isBlank() ? "TODOS" : periodo.toUpperCase();
        return switch (periodoNormalizado) {
            case "FUTUROS" -> !item.getDataFinal().isBefore(hoje);
            case "PASSADOS" -> item.getDataFinal().isBefore(hoje);
            default -> true;
        };
    }

    private void validarEdicaoPermitida(BloqueioAgendaEntity entity) {
        if (entity.isCancelado()) {
            throw new BusinessException("Bloqueios cancelados permanecem apenas como histórico e não podem ser alterados.");
        }
        if (entity.getDataFinal().isBefore(LocalDate.now())) {
            throw new BusinessException("Bloqueios passados não podem ser alterados ou cancelados.");
        }
    }

    private List<BloqueioAgendaEntity> grupo(BloqueioAgendaEntity base) {
        if (base.getGrupoReferencia() == null) {
            return List.of(base);
        }
        return bloqueioAgendaRepository.findByGrupoReferenciaOrderByDataHoraInicioAsc(base.getGrupoReferencia());
    }

    private BloqueioAgendaEntity buscarPorId(UUID id) {
        return bloqueioAgendaRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Bloqueio não encontrado."));
    }

    private ConfiguracaoClinicaEntity configuracao() {
        return configuracaoClinicaRepository.findFirstByAtivoTrueOrderByCreatedAtAsc()
                .orElseThrow(() -> new BusinessException("Configure a clínica antes de gerenciar bloqueios."));
    }

    private ZoneId zoneId() {
        return ZoneId.of(configuracao().getTimezoneId());
    }

    private String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private LinkedHashMap<String, Object> snapshotGrupo(List<BloqueioAgendaEntity> grupo) {
        LinkedHashMap<String, Object> snapshot = new LinkedHashMap<>();
        if (grupo == null || grupo.isEmpty()) {
            return snapshot;
        }

        BloqueioAgendaEntity primeiro = grupo.getFirst();
        BloqueioAgendaEntity ultimo = grupo.getLast();
        snapshot.put("grupoReferencia", primeiro.getGrupoReferencia() != null ? primeiro.getGrupoReferencia() : primeiro.getId());
        snapshot.put("quantidadeRegistros", grupo.size());
        snapshot.put("dataHoraInicio", primeiro.getDataHoraInicio());
        snapshot.put("dataHoraFim", ultimo.getDataHoraFim());
        snapshot.put("diaInteiro", primeiro.getDiaInteiro());
        snapshot.put("motivo", primeiro.getMotivo());
        snapshot.put("observacao", primeiro.getObservacao());
        snapshot.put("status", primeiro.getStatus());
        snapshot.put("origem", primeiro.getOrigem());
        snapshot.put("ativo", primeiro.getAtivo());
        return snapshot;
    }

    private String resolveAuditEntityId(List<BloqueioAgendaEntity> grupo) {
        if (grupo == null || grupo.isEmpty()) {
            return null;
        }
        BloqueioAgendaEntity primeiro = grupo.getFirst();
        UUID auditId = primeiro.getGrupoReferencia() != null ? primeiro.getGrupoReferencia() : primeiro.getId();
        return auditId != null ? auditId.toString() : null;
    }

    private String describeAuditRecord(List<BloqueioAgendaEntity> grupo) {
        if (grupo == null || grupo.isEmpty()) {
            return "Bloqueio de agenda";
        }
        BloqueioAgendaEntity primeiro = grupo.getFirst();
        if (grupo.size() == 1) {
            return texto(primeiro.getMotivo()) != null ? primeiro.getMotivo() : "Bloqueio de agenda";
        }
        return (texto(primeiro.getMotivo()) != null ? primeiro.getMotivo() : "Bloqueio recorrente")
                + " (" + grupo.size() + " registros)";
    }

    private AuditEventOrigin mapAuditOrigin(BloqueioAgendaOrigem origem) {
        if (origem == null) {
            return AuditEventOrigin.MANUAL;
        }
        return switch (origem) {
            case ONLINE -> AuditEventOrigin.ONLINE;
            case SISTEMA -> AuditEventOrigin.SISTEMA;
            default -> AuditEventOrigin.MANUAL;
        };
    }

    private record Intervalo(OffsetDateTime inicio, OffsetDateTime fim) {}

    public record ResumoBloqueio(long total, long ativos, long diaInteiro) {}

    public record BloqueioEdicaoView(BloqueioAgendaEntity entidade, BloqueioAgendaCadastroForm form, List<BloqueioAgendaEntity> grupo) {}
}
