package com.fisiogestao.pacientes.service;

import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.agenda.SessaoStatus;
import com.fisiogestao.agenda.TipoAgendamento;
import com.fisiogestao.agenda.form.AgendaManualForm;
import com.fisiogestao.agenda.repository.SessaoRepository;
import com.fisiogestao.agenda.service.AgendaService;
import com.fisiogestao.pacientes.dto.AtendimentoAvulsoDTO;
import com.fisiogestao.pacientes.dto.AtendimentoAvulsoFormDTO;
import com.fisiogestao.pacientes.dto.ResumoAvulsoDTO;
import com.fisiogestao.pacientes.dto.SessaoHistoricoDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessaoConsumoService {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy '·' HH'h'mm");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final SessaoRepository sessaoRepository;
    private final AgendaService agendaService;

    @Transactional(readOnly = true)
    public List<SessaoHistoricoDTO> historicoPorPaciente(UUID pacienteId) {
        return sessaoRepository.findByClienteIdOrderByDataHoraInicioDesc(pacienteId).stream()
                .filter(sessao -> sessao.getTipoAgendamento() == TipoAgendamento.PLANO)
                .limit(8)
                .map(sessao -> new SessaoHistoricoDTO(descricao(sessao), sessao.getDataHoraInicio().format(DATA_HORA), sessao.getStatus() == SessaoStatus.REALIZADA, procedimento(sessao), sessao.getObservacoes(), sessao.getFeedbackEvaPaciente(), sessao.getFeedbackObsPaciente()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SessaoHistoricoDTO> historicoPorPlano(UUID planoId) {
        return sessaoRepository.findByPlanoSessaoIdOrderByDataHoraInicioDesc(planoId).stream()
                .limit(8)
                .map(sessao -> new SessaoHistoricoDTO(descricao(sessao), sessao.getDataHoraInicio().format(DATA_HORA), sessao.getStatus() == SessaoStatus.REALIZADA, procedimento(sessao), sessao.getObservacoes(), sessao.getFeedbackEvaPaciente(), sessao.getFeedbackObsPaciente()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AtendimentoAvulsoDTO> historicoAvulsoPorPaciente(UUID pacienteId) {
        return sessoesAvulsas(pacienteId).stream()
                .limit(6)
                .map(sessao -> new AtendimentoAvulsoDTO(
                        procedimento(sessao),
                        sessao.getDataHoraInicio().format(DATA),
                        dinheiro(sessao.getValorAvulso())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ResumoAvulsoDTO resumoAvulso(UUID pacienteId) {
        return sessoesAvulsas(pacienteId).stream()
                .findFirst()
                .map(sessao -> new ResumoAvulsoDTO(
                        procedimento(sessao),
                        sessao.getDataHoraInicio().format(DATA_HORA),
                        "Registrado",
                        dinheiro(sessao.getValorAvulso())))
                .orElse(new ResumoAvulsoDTO(null, null, null, dinheiro(BigDecimal.ZERO)));
    }

    @Transactional(readOnly = true)
    public int countAgendadasPorPlano(UUID planoId) {
        return (int) sessaoRepository.countAgendadasPorPlano(planoId);
    }

    @Transactional
    public void registrarAvulso(UUID pacienteId, AtendimentoAvulsoFormDTO form, String usuario) {
        AgendaManualForm agenda = new AgendaManualForm();
        agenda.setClienteId(pacienteId);
        agenda.setData(form.getDataAtendimento() == null ? LocalDate.now() : form.getDataAtendimento());
        agenda.setHorario(form.getHorario() == null ? LocalTime.of(8, 0) : form.getHorario());
        agenda.setTipoAtendimento(procedimento(form.getProcedimento()));
        agenda.setTipoAgendamento(TipoAgendamento.AVULSO);
        agenda.setValorAvulso(form.getValorCobrado() == null ? BigDecimal.ZERO : form.getValorCobrado());
        agenda.setObservacoes(form.getObservacoes());
        agendaService.agendarSessaoManual(agenda, usuario == null ? "Equipe da clínica" : usuario);
    }

    private List<SessaoEntity> sessoesAvulsas(UUID pacienteId) {
        return sessaoRepository.findByClienteIdOrderByDataHoraInicioDesc(pacienteId).stream()
                .filter(sessao -> sessao.getTipoAgendamento() == TipoAgendamento.AVULSO)
                .sorted(Comparator.comparing(SessaoEntity::getDataHoraInicio).reversed())
                .toList();
    }

    private String descricao(SessaoEntity sessao) {
        if (sessao.getStatus() == SessaoStatus.REALIZADA) {
            return "Sessão realizada";
        }
        if (sessao.getStatus() == SessaoStatus.CANCELADA) {
            return "Sessão cancelada";
        }
        return "Sessão agendada";
    }

    private String procedimento(SessaoEntity sessao) {
        return procedimento(sessao.getTipoAtendimento());
    }

    private String procedimento(String valor) {
        return valor == null || valor.isBlank() ? "Fisioterapia geral" : valor.trim();
    }

    private String dinheiro(BigDecimal valor) {
        return NumberFormat.getCurrencyInstance(PT_BR).format(valor == null ? BigDecimal.ZERO : valor);
    }
}
