package com.fisiogestao.relatorios.service;

import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.agenda.service.AgendaService;
import com.fisiogestao.config.TimezoneConfig;
import com.fisiogestao.email.EmailLogType;
import com.fisiogestao.email.service.EmailAttachment;
import com.fisiogestao.email.service.EmailDispatchResult;
import com.fisiogestao.email.service.EmailRequest;
import com.fisiogestao.email.service.SystemEmailService;
import com.fisiogestao.financeiro.LancamentoFinanceiroEntity;
import com.fisiogestao.financeiro.TipoLancamentoFinanceiro;
import com.fisiogestao.financeiro.service.FinanceiroService;
import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.pacientes.service.PacienteService;
import com.fisiogestao.relatorios.form.CobrancaEmailForm;
import com.fisiogestao.relatorios.form.EnvioRelatorioForm;
import com.fisiogestao.relatorios.form.FiltroRelatorioForm;
import com.fisiogestao.relatorios.form.FormatoRelatorio;
import com.fisiogestao.relatorios.form.LembreteEmailForm;
import com.fisiogestao.shared.web.BusinessException;
import com.fisiogestao.solicitacoes.SolicitacaoAgendamentoEntity;
import com.fisiogestao.solicitacoes.service.SolicitacaoAgendamentoService;
import com.lowagie.text.Document;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RelatoriosService {

    private final PacienteService pacienteService;
    private final AgendaService agendaService;
    private final FinanceiroService financeiroService;
    private final SolicitacaoAgendamentoService solicitacaoAgendamentoService;
    private final SystemEmailService systemEmailService;

    public RelatorioOperacional gerarRelatorio(FiltroRelatorioForm filtro) {
        validarFiltro(filtro);

        List<ClienteEntity> pacientes = filtro.isIncluirPacientes() ? pacienteService.listarAtivos() : List.of();
        List<SessaoEntity> sessoes = filtro.isIncluirAgenda()
                ? agendaService.listarSessoesNoPeriodo(filtro.getDataInicial(), filtro.getDataFinal().plusDays(1))
                : List.of();
        List<LancamentoFinanceiroEntity> lancamentos = filtro.isIncluirFinanceiro()
                ? financeiroService.listarTodos().stream()
                .filter(item -> !item.getDataCompetencia().toLocalDate().isBefore(filtro.getDataInicial()))
                .filter(item -> !item.getDataCompetencia().toLocalDate().isAfter(filtro.getDataFinal()))
                .toList()
                : List.of();
        List<SolicitacaoAgendamentoEntity> solicitacoes = filtro.isIncluirSolicitacoes()
                ? solicitacaoAgendamentoService.listarNoPeriodo(filtro.getDataInicial(), filtro.getDataFinal().plusDays(1))
                : List.of();

        BigDecimal totalReceitas = somarPorTipo(lancamentos, TipoLancamentoFinanceiro.RECEITA);
        BigDecimal totalDespesas = somarPorTipo(lancamentos, TipoLancamentoFinanceiro.DESPESA);

        return new RelatorioOperacional(filtro, pacientes, sessoes, lancamentos, solicitacoes, totalReceitas, totalDespesas);
    }

    public RelatorioDocumento exportar(RelatorioOperacional relatorio, FormatoRelatorio formato) {
        return switch (formato) {
            case PDF -> gerarPdf(relatorio);
            case EXCEL -> gerarExcel(relatorio);
        };
    }

    public EmailDispatchResult enviarRelatorioPorEmail(RelatorioOperacional relatorio, EnvioRelatorioForm form, String usuario) {
        RelatorioDocumento documento = exportar(relatorio, form.getFormato());
        return systemEmailService.send(EmailRequest.builder()
                .type(EmailLogType.RELATORIO)
                .subject(form.getAssunto())
                .to(form.getDestinatario())
                .templateName("email/relatorio")
                .templateId("relatorio")
                .triggeredBy(usuario)
                .entityType("relatorio_operacional")
                .templateVariables(Map.of(
                        "titulo", form.getAssunto(),
                        "mensagem", texto(form.getMensagem(), "Segue em anexo o relatório solicitado no sistema."),
                        "dataInicial", relatorio.filtro().getDataInicial(),
                        "dataFinal", relatorio.filtro().getDataFinal()
                ))
                .attachment(new EmailAttachment(documento.fileName(), documento.contentType(), documento.content()))
                .build());
    }

    public EmailDispatchResult enviarLembrete(LembreteEmailForm form, String usuario) {
        return systemEmailService.send(EmailRequest.builder()
                .type(EmailLogType.LEMBRETE)
                .subject(form.getAssunto())
                .to(form.getEmailDestinatario())
                .templateName("email/lembrete")
                .templateId("lembrete")
                .triggeredBy(usuario)
                .entityType("lembrete")
                .templateVariables(Map.of(
                        "titulo", form.getAssunto(),
                        "mensagem", texto(form.getMensagem(), "Este e-mail foi enviado como lembrete pelo sistema."),
                        "nomeDestinatario", texto(form.getNomeDestinatario(), "Paciente"),
                        "tipoLembrete", form.getTipo().name(),
                        "dataReferencia", form.getDataReferencia(),
                        "horaReferencia", form.getHoraReferencia()
                ))
                .build());
    }

    public EmailDispatchResult enviarCobranca(CobrancaEmailForm form, String usuario) {
        LancamentoFinanceiroEntity lancamento = financeiroService.buscarPorId(form.getLancamentoId());
        RelatorioDocumento anexo = gerarPdfCobranca(lancamento);
        return systemEmailService.send(EmailRequest.builder()
                .type(EmailLogType.COBRANCA)
                .subject(form.getAssunto())
                .to(form.getEmailDestinatario())
                .templateName("email/cobranca")
                .templateId("cobranca")
                .triggeredBy(usuario)
                .entityType("lancamento_financeiro")
                .entityId(lancamento.getId())
                .templateVariables(Map.of(
                        "titulo", form.getAssunto(),
                        "mensagem", texto(form.getMensagem(), "Segue o demonstrativo financeiro referente ao lançamento informado."),
                        "descricao", lancamento.getDescricao(),
                        "valor", lancamento.getValor(),
                        "dataCompetencia", lancamento.getDataCompetencia()
                ))
                .attachment(new EmailAttachment(anexo.fileName(), anexo.contentType(), anexo.content()))
                .build());
    }

    private RelatorioDocumento gerarPdf(RelatorioOperacional relatorio) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, output);
            document.open();
            document.add(new Paragraph("Relatório Operacional - FisioGestao", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
            document.add(new Paragraph("Período: " + relatorio.filtro().getDataInicial() + " até " + relatorio.filtro().getDataFinal()));
            document.add(new Paragraph(" "));

            PdfPTable resumo = new PdfPTable(4);
            resumo.setWidthPercentage(100);
            resumo.addCell(celulaCabecalho("Pacientes"));
            resumo.addCell(celulaCabecalho("Sessões"));
            resumo.addCell(celulaCabecalho("Receitas"));
            resumo.addCell(celulaCabecalho("Despesas"));
            resumo.addCell(celulaValor(String.valueOf(relatorio.pacientes().size())));
            resumo.addCell(celulaValor(String.valueOf(relatorio.sessoes().size())));
            resumo.addCell(celulaValor(formatCurrency(relatorio.totalReceitas())));
            resumo.addCell(celulaValor(formatCurrency(relatorio.totalDespesas())));
            document.add(resumo);
            document.add(new Paragraph(" "));

            if (!relatorio.sessoes().isEmpty()) {
                document.add(new Paragraph("Sessões", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
                document.add(tabelaSessoes(relatorio.sessoes()));
            }
            if (!relatorio.lancamentos().isEmpty()) {
                document.add(new Paragraph(" "));
                document.add(new Paragraph("Financeiro", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
                document.add(tabelaFinanceiro(relatorio.lancamentos()));
            }
            if (!relatorio.solicitacoes().isEmpty()) {
                document.add(new Paragraph(" "));
                document.add(new Paragraph("Solicitações", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
                document.add(tabelaSolicitacoes(relatorio.solicitacoes()));
            }
            document.close();
            return new RelatorioDocumento("relatorio-operacional.pdf", "application/pdf", output.toByteArray());
        } catch (Exception ex) {
            throw new BusinessException("Não foi possível gerar o PDF do relatório.");
        }
    }

    private RelatorioDocumento gerarExcel(RelatorioOperacional relatorio) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet resumo = workbook.createSheet("Resumo");
            Row row = resumo.createRow(0);
            row.createCell(0).setCellValue("Período");
            row.createCell(1).setCellValue(relatorio.filtro().getDataInicial() + " até " + relatorio.filtro().getDataFinal());
            row = resumo.createRow(1);
            row.createCell(0).setCellValue("Pacientes");
            row.createCell(1).setCellValue(relatorio.pacientes().size());
            row = resumo.createRow(2);
            row.createCell(0).setCellValue("Sessões");
            row.createCell(1).setCellValue(relatorio.sessoes().size());
            row = resumo.createRow(3);
            row.createCell(0).setCellValue("Receitas");
            row.createCell(1).setCellValue(relatorio.totalReceitas().doubleValue());
            row = resumo.createRow(4);
            row.createCell(0).setCellValue("Despesas");
            row.createCell(1).setCellValue(relatorio.totalDespesas().doubleValue());

            if (!relatorio.sessoes().isEmpty()) {
                popularAgendaSheet(workbook.createSheet("Agenda"), relatorio.sessoes());
            }
            if (!relatorio.lancamentos().isEmpty()) {
                popularFinanceiroSheet(workbook.createSheet("Financeiro"), relatorio.lancamentos());
            }
            if (!relatorio.solicitacoes().isEmpty()) {
                popularSolicitacaoSheet(workbook.createSheet("Solicitacoes"), relatorio.solicitacoes());
            }

            workbook.write(output);
            return new RelatorioDocumento("relatorio-operacional.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    output.toByteArray());
        } catch (Exception ex) {
            throw new BusinessException("Não foi possível gerar a planilha do relatório.");
        }
    }

    private RelatorioDocumento gerarPdfCobranca(LancamentoFinanceiroEntity lancamento) {
        FiltroRelatorioForm filtro = new FiltroRelatorioForm();
        filtro.setDataInicial(lancamento.getDataCompetencia().toLocalDate());
        filtro.setDataFinal(lancamento.getDataCompetencia().toLocalDate());
        filtro.setIncluirPacientes(false);
        filtro.setIncluirAgenda(false);
        filtro.setIncluirFinanceiro(true);
        filtro.setIncluirSolicitacoes(false);
        return gerarPdf(new RelatorioOperacional(
                filtro,
                List.of(),
                List.of(),
                List.of(lancamento),
                List.of(),
                lancamento.getTipo() == TipoLancamentoFinanceiro.RECEITA ? lancamento.getValor() : BigDecimal.ZERO,
                lancamento.getTipo() == TipoLancamentoFinanceiro.DESPESA ? lancamento.getValor() : BigDecimal.ZERO
        ));
    }

    private BigDecimal somarPorTipo(List<LancamentoFinanceiroEntity> lancamentos, TipoLancamentoFinanceiro tipo) {
        return lancamentos.stream()
                .filter(item -> item.getTipo() == tipo)
                .map(LancamentoFinanceiroEntity::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private PdfPTable tabelaSessoes(List<SessaoEntity> sessoes) {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.addCell(celulaCabecalho("Paciente"));
        table.addCell(celulaCabecalho("Início"));
        table.addCell(celulaCabecalho("Fim"));
        table.addCell(celulaCabecalho("Status"));
        for (SessaoEntity item : sessoes) {
            table.addCell(celulaValor(item.getCliente().getNomeCompleto()));
            table.addCell(celulaValor(formatDateTime(item.getDataHoraInicio())));
            table.addCell(celulaValor(formatDateTime(item.getDataHoraFim())));
            table.addCell(celulaValor(item.getStatus().name()));
        }
        return table;
    }

    private PdfPTable tabelaFinanceiro(List<LancamentoFinanceiroEntity> lancamentos) {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.addCell(celulaCabecalho("Descrição"));
        table.addCell(celulaCabecalho("Tipo"));
        table.addCell(celulaCabecalho("Status"));
        table.addCell(celulaCabecalho("Valor"));
        for (LancamentoFinanceiroEntity item : lancamentos) {
            table.addCell(celulaValor(item.getDescricao()));
            table.addCell(celulaValor(item.getTipo().name()));
            table.addCell(celulaValor(item.getStatus().name()));
            table.addCell(celulaValor(formatCurrency(item.getValor())));
        }
        return table;
    }

    private PdfPTable tabelaSolicitacoes(List<SolicitacaoAgendamentoEntity> solicitacoes) {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.addCell(celulaCabecalho("Paciente"));
        table.addCell(celulaCabecalho("Data"));
        table.addCell(celulaCabecalho("Telefone"));
        table.addCell(celulaCabecalho("Status"));
        for (SolicitacaoAgendamentoEntity item : solicitacoes) {
            table.addCell(celulaValor(item.getNomePaciente()));
            table.addCell(celulaValor(formatDateTime(item.getDataHoraInicio())));
            table.addCell(celulaValor(item.getTelefone()));
            table.addCell(celulaValor(item.getStatus().name()));
        }
        return table;
    }

    private void popularAgendaSheet(Sheet sheet, List<SessaoEntity> sessoes) {
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Paciente");
        header.createCell(1).setCellValue("Início");
        header.createCell(2).setCellValue("Fim");
        header.createCell(3).setCellValue("Status");
        int rowIndex = 1;
        for (SessaoEntity item : sessoes) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(item.getCliente().getNomeCompleto());
            row.createCell(1).setCellValue(formatDateTime(item.getDataHoraInicio()));
            row.createCell(2).setCellValue(formatDateTime(item.getDataHoraFim()));
            row.createCell(3).setCellValue(item.getStatus().name());
        }
    }

    private void popularFinanceiroSheet(Sheet sheet, List<LancamentoFinanceiroEntity> lancamentos) {
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Descrição");
        header.createCell(1).setCellValue("Tipo");
        header.createCell(2).setCellValue("Status");
        header.createCell(3).setCellValue("Competência");
        header.createCell(4).setCellValue("Valor");
        int rowIndex = 1;
        for (LancamentoFinanceiroEntity item : lancamentos) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(item.getDescricao());
            row.createCell(1).setCellValue(item.getTipo().name());
            row.createCell(2).setCellValue(item.getStatus().name());
            row.createCell(3).setCellValue(formatDateTime(item.getDataCompetencia()));
            row.createCell(4).setCellValue(item.getValor().doubleValue());
        }
    }

    private void popularSolicitacaoSheet(Sheet sheet, List<SolicitacaoAgendamentoEntity> solicitacoes) {
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Paciente");
        header.createCell(1).setCellValue("Data");
        header.createCell(2).setCellValue("Telefone");
        header.createCell(3).setCellValue("Status");
        int rowIndex = 1;
        for (SolicitacaoAgendamentoEntity item : solicitacoes) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(item.getNomePaciente());
            row.createCell(1).setCellValue(formatDateTime(item.getDataHoraInicio()));
            row.createCell(2).setCellValue(item.getTelefone());
            row.createCell(3).setCellValue(item.getStatus().name());
        }
    }

    private PdfPCell celulaCabecalho(String valor) {
        PdfPCell cell = new PdfPCell(new Phrase(valor, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9)));
        cell.setPadding(6f);
        return cell;
    }

    private PdfPCell celulaValor(String valor) {
        PdfPCell cell = new PdfPCell(new Phrase(valor == null ? "-" : valor, FontFactory.getFont(FontFactory.HELVETICA, 9)));
        cell.setPadding(6f);
        return cell;
    }

    private void validarFiltro(FiltroRelatorioForm filtro) {
        if (filtro.getDataInicial() == null || filtro.getDataFinal() == null) {
            throw new BusinessException("Informe o período inicial e final do relatório.");
        }
        if (filtro.getDataFinal().isBefore(filtro.getDataInicial())) {
            throw new BusinessException("A data final não pode ser anterior à data inicial.");
        }
    }

    private String formatDateTime(OffsetDateTime dataHora) {
        return dataHora.atZoneSameInstant(ZoneId.of(TimezoneConfig.CLINIC_TIMEZONE))
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    private String formatCurrency(BigDecimal value) {
        return "R$ " + value.setScale(2, RoundingMode.HALF_UP);
    }

    private String texto(String valor, String padrao) {
        return valor == null || valor.isBlank() ? padrao : valor.trim();
    }
}
