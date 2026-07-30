package com.fisiogestao.relatorios.service;

import com.fisiogestao.agenda.service.AgendaService;
import com.fisiogestao.auth.MenuAccessSecurity;
import com.fisiogestao.email.service.SystemEmailService;
import com.fisiogestao.financeiro.service.FinanceiroService;
import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.pacientes.service.PacienteService;
import com.fisiogestao.relatorios.form.FiltroRelatorioForm;
import com.fisiogestao.shared.web.BusinessException;
import com.fisiogestao.solicitacoes.service.SolicitacaoAgendamentoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    @Mock
    private MenuAccessSecurity menuAccessSecurity;

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

    @Test
    void gerarFaturamentoDetalhadoRetornaNuloSemPermissaoFaturamento() {
        when(menuAccessSecurity.canAccess("FATURAMENTO")).thenReturn(false);

        FiltroRelatorioForm filtro = filtroSomentePacientes();
        filtro.setIncluirFinanceiro(true);
        assertNull(relatoriosService.gerarFaturamentoDetalhado(filtro));
        verify(financeiroService, never()).gerarRelatorioFaturamento(any(), any());
    }

    @Test
    void gerarFaturamentoDetalhadoRetornaNuloQuandoFinanceiroExcluidoDoFiltro() {
        FiltroRelatorioForm filtro = filtroSomentePacientes();
        filtro.setIncluirFinanceiro(false);

        assertNull(relatoriosService.gerarFaturamentoDetalhado(filtro));
        verify(financeiroService, never()).gerarRelatorioFaturamento(any(), any());
    }

    @Test
    void gerarFaturamentoDetalhadoConsultaFinanceiroQuandoAutorizado() {
        when(menuAccessSecurity.canAccess("FATURAMENTO")).thenReturn(true);
        FiltroRelatorioForm filtro = filtroSomentePacientes();
        filtro.setIncluirFinanceiro(true);
        FinanceiroService.RelatorioFaturamento relatorioFaturamento = new FinanceiroService.RelatorioFaturamento(
                filtro.getDataInicial(), filtro.getDataFinal(),
                new FinanceiroService.FaturamentoResumo(BigDecimal.TEN, 1L, BigDecimal.TEN),
                List.of(), List.of(), List.of(), List.of(), List.of());
        FinanceiroService.InadimplenciaResumo inadimplencia = new FinanceiroService.InadimplenciaResumo(BigDecimal.ZERO, 0L, 0L);
        when(financeiroService.gerarRelatorioFaturamento(filtro.getDataInicial(), filtro.getDataFinal()))
                .thenReturn(relatorioFaturamento);
        when(financeiroService.calcularInadimplencia()).thenReturn(inadimplencia);

        RelatoriosService.FaturamentoDetalhado resultado = relatoriosService.gerarFaturamentoDetalhado(filtro);

        assertEquals(relatorioFaturamento, resultado.relatorio());
        assertEquals(inadimplencia, resultado.inadimplencia());
    }

    @Test
    void gerarFaturamentoDetalhadoRejeitaPeriodoInvertido() {
        when(menuAccessSecurity.canAccess("FATURAMENTO")).thenReturn(true);
        FiltroRelatorioForm filtro = filtroSomentePacientes();
        filtro.setIncluirFinanceiro(true);
        filtro.setDataInicial(LocalDate.of(2026, 5, 20));
        filtro.setDataFinal(LocalDate.of(2026, 5, 1));

        assertThrows(BusinessException.class, () -> relatoriosService.gerarFaturamentoDetalhado(filtro));
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
