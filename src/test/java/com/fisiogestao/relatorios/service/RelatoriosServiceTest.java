package com.fisiogestao.relatorios.service;

import com.fisiogestao.agenda.service.AgendaService;
import com.fisiogestao.email.service.SystemEmailService;
import com.fisiogestao.financeiro.service.FinanceiroService;
import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.pacientes.service.PacienteService;
import com.fisiogestao.relatorios.form.FiltroRelatorioForm;
import com.fisiogestao.solicitacoes.service.SolicitacaoAgendamentoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelatoriosServiceTest {

    @Mock
    private PacienteService pacienteService;
    @Mock
    private AgendaService agendaService;
    @Mock
    private FinanceiroService financeiroService;
    @Mock
    private SolicitacaoAgendamentoService solicitacaoAgendamentoService;
    @Mock
    private SystemEmailService systemEmailService;
    @Spy
    private RelatorioAnalyticsBuilder analyticsBuilder = new RelatorioAnalyticsBuilder();
    @Mock
    private RelatorioExportService exportService;

    @InjectMocks
    private RelatoriosService relatoriosService;

    @Test
    void gerarRelatorioNaoCarregaTodosPacientesAtivos() {
        ClienteEntity recente = ClienteEntity.builder()
                .nomeCompleto("Ana Paciente")
                .telefone("65999999999")
                .email("ana@example.com")
                .build();
        recente.setCreatedAt(OffsetDateTime.parse("2026-05-10T09:00:00-04:00"));

        when(pacienteService.contarAtivos()).thenReturn(42L);
        when(pacienteService.contarAtivosCriadosNoPeriodo(any(), any())).thenReturn(3L, 2L);
        when(pacienteService.listarAtivosRecentes()).thenReturn(List.of(recente));

        RelatorioOperacional relatorio = relatoriosService.gerarRelatorio(filtroSomentePacientes());

        assertEquals(42L, relatorio.totalPacientesAtivos());
        assertEquals(1, relatorio.analytics().pacientesRecentes().size());
        assertEquals("Ana Paciente", relatorio.analytics().pacientesRecentes().getFirst().nome());
        assertEquals("42", relatorio.analytics().metricas().getFirst().value());
        verify(pacienteService, never()).listarAtivos();
    }

    private FiltroRelatorioForm filtroSomentePacientes() {
        FiltroRelatorioForm filtro = new FiltroRelatorioForm();
        filtro.setDataInicial(LocalDate.of(2026, 5, 1));
        filtro.setDataFinal(LocalDate.of(2026, 5, 20));
        filtro.setIncluirPacientes(true);
        filtro.setIncluirAgenda(false);
        filtro.setIncluirFinanceiro(false);
        filtro.setIncluirSolicitacoes(false);
        return filtro;
    }
}
