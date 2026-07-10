package com.fisiogestao.relatorios.service;

import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.financeiro.LancamentoFinanceiroEntity;
import com.fisiogestao.relatorios.form.FormatoRelatorio;
import com.fisiogestao.shared.web.BusinessException;
import com.fisiogestao.solicitacoes.SolicitacaoAgendamentoEntity;
import com.lowagie.text.Document;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
public class RelatorioExportService {

    public RelatorioDocumento exportar(RelatorioOperacional relatorio, FormatoRelatorio formato) {
        return switch (formato) {
            case PDF   -> gerarPdf(relatorio);
            case EXCEL -> gerarExcel(relatorio);
        };
    }

    // ── PDF ───────────────────────────────────────────────────────────────────

    RelatorioDocumento gerarPdf(RelatorioOperacional relatorio) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, output);
            document.open();

            document.add(new Paragraph("Relatório Operacional - FisioGestao",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
            document.add(new Paragraph("Período: " + relatorio.filtro().getDataInicial() +
                    " até " + relatorio.filtro().getDataFinal()));
            document.add(new Paragraph(" "));

            PdfPTable resumo = new PdfPTable(5);
            resumo.setWidthPercentage(100);
            resumo.addCell(celulaCabecalho("Pacientes"));
            resumo.addCell(celulaCabecalho("Sessões"));
            resumo.addCell(celulaCabecalho("Receitas"));
            resumo.addCell(celulaCabecalho("Despesas"));
            resumo.addCell(celulaCabecalho("Saldo"));
            resumo.addCell(celulaValor(String.valueOf(relatorio.totalPacientesAtivos())));
            resumo.addCell(celulaValor(String.valueOf(relatorio.sessoes().size())));
            resumo.addCell(celulaValor(RelatorioFormatUtil.formatCurrency(relatorio.totalReceitas())));
            resumo.addCell(celulaValor(RelatorioFormatUtil.formatCurrency(relatorio.totalDespesas())));
            resumo.addCell(celulaValor(RelatorioFormatUtil.formatCurrency(relatorio.saldoFinanceiro())));
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

    // ── Excel ─────────────────────────────────────────────────────────────────

    private RelatorioDocumento gerarExcel(RelatorioOperacional relatorio) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet resumo = workbook.createSheet("Resumo");
            Row row = resumo.createRow(0);
            row.createCell(0).setCellValue("Período");
            row.createCell(1).setCellValue(relatorio.filtro().getDataInicial() + " até " + relatorio.filtro().getDataFinal());
            row = resumo.createRow(1);
            row.createCell(0).setCellValue("Pacientes"); row.createCell(1).setCellValue(relatorio.totalPacientesAtivos());
            row = resumo.createRow(2);
            row.createCell(0).setCellValue("Sessões");   row.createCell(1).setCellValue(relatorio.sessoes().size());
            row = resumo.createRow(3);
            row.createCell(0).setCellValue("Receitas");  row.createCell(1).setCellValue(relatorio.totalReceitas().doubleValue());
            row = resumo.createRow(4);
            row.createCell(0).setCellValue("Despesas");  row.createCell(1).setCellValue(relatorio.totalDespesas().doubleValue());
            row = resumo.createRow(5);
            row.createCell(0).setCellValue("Saldo");     row.createCell(1).setCellValue(relatorio.saldoFinanceiro().doubleValue());

            if (!relatorio.sessoes().isEmpty())      popularAgendaSheet(workbook.createSheet("Agenda"), relatorio.sessoes());
            if (!relatorio.lancamentos().isEmpty())  popularFinanceiroSheet(workbook.createSheet("Financeiro"), relatorio.lancamentos());
            if (!relatorio.solicitacoes().isEmpty()) popularSolicitacaoSheet(workbook.createSheet("Solicitacoes"), relatorio.solicitacoes());

            workbook.write(output);
            return new RelatorioDocumento(
                    "relatorio-operacional.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    output.toByteArray()
            );
        } catch (Exception ex) {
            throw new BusinessException("Não foi possível gerar a planilha do relatório.");
        }
    }

    // ── Tabelas PDF ───────────────────────────────────────────────────────────

    private PdfPTable tabelaSessoes(List<SessaoEntity> sessoes) {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.addCell(celulaCabecalho("Paciente"));
        table.addCell(celulaCabecalho("Início"));
        table.addCell(celulaCabecalho("Fim"));
        table.addCell(celulaCabecalho("Status"));
        for (SessaoEntity item : sessoes) {
            table.addCell(celulaValor(item.getCliente().getNomeCompleto()));
            table.addCell(celulaValor(RelatorioFormatUtil.formatDateTime(item.getDataHoraInicio())));
            table.addCell(celulaValor(RelatorioFormatUtil.formatDateTime(item.getDataHoraFim())));
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
            table.addCell(celulaValor(RelatorioFormatUtil.formatCurrency(item.getValor())));
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
            table.addCell(celulaValor(RelatorioFormatUtil.formatDateTime(item.getDataHoraInicio())));
            table.addCell(celulaValor(item.getTelefone()));
            table.addCell(celulaValor(item.getStatus().name()));
        }
        return table;
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

    // ── Sheets Excel ──────────────────────────────────────────────────────────

    private void popularAgendaSheet(Sheet sheet, List<SessaoEntity> sessoes) {
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Paciente");
        header.createCell(1).setCellValue("Início");
        header.createCell(2).setCellValue("Fim");
        header.createCell(3).setCellValue("Status");
        int idx = 1;
        for (SessaoEntity item : sessoes) {
            Row r = sheet.createRow(idx++);
            r.createCell(0).setCellValue(item.getCliente().getNomeCompleto());
            r.createCell(1).setCellValue(RelatorioFormatUtil.formatDateTime(item.getDataHoraInicio()));
            r.createCell(2).setCellValue(RelatorioFormatUtil.formatDateTime(item.getDataHoraFim()));
            r.createCell(3).setCellValue(item.getStatus().name());
        }
    }

    private void popularFinanceiroSheet(Sheet sheet, List<LancamentoFinanceiroEntity> lancamentos) {
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Descrição");
        header.createCell(1).setCellValue("Tipo");
        header.createCell(2).setCellValue("Status");
        header.createCell(3).setCellValue("Recebimento");
        header.createCell(4).setCellValue("Valor");
        int idx = 1;
        for (LancamentoFinanceiroEntity item : lancamentos) {
            Row r = sheet.createRow(idx++);
            r.createCell(0).setCellValue(item.getDescricao());
            r.createCell(1).setCellValue(item.getTipo().name());
            r.createCell(2).setCellValue(item.getStatus().name());
            r.createCell(3).setCellValue(RelatorioFormatUtil.formatDateTime(item.getDataRecebimento()));
            r.createCell(4).setCellValue(item.getValor().doubleValue());
        }
    }

    private void popularSolicitacaoSheet(Sheet sheet, List<SolicitacaoAgendamentoEntity> solicitacoes) {
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Paciente");
        header.createCell(1).setCellValue("Data");
        header.createCell(2).setCellValue("Telefone");
        header.createCell(3).setCellValue("Status");
        int idx = 1;
        for (SolicitacaoAgendamentoEntity item : solicitacoes) {
            Row r = sheet.createRow(idx++);
            r.createCell(0).setCellValue(item.getNomePaciente());
            r.createCell(1).setCellValue(RelatorioFormatUtil.formatDateTime(item.getDataHoraInicio()));
            r.createCell(2).setCellValue(item.getTelefone());
            r.createCell(3).setCellValue(item.getStatus().name());
        }
    }
}
