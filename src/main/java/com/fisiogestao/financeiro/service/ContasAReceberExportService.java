package com.fisiogestao.financeiro.service;

import com.fisiogestao.relatorios.form.FormatoRelatorio;
import com.fisiogestao.relatorios.service.RelatorioDocumento;
import com.fisiogestao.shared.web.BusinessException;
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
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class ContasAReceberExportService {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter FMT_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public RelatorioDocumento exportar(FinanceiroService.ContasAReceberView view, FormatoRelatorio formato) {
        return switch (formato) {
            case PDF   -> gerarPdf(view);
            case EXCEL -> gerarExcel(view);
        };
    }

    // ── PDF ───────────────────────────────────────────────────────────────────

    private RelatorioDocumento gerarPdf(FinanceiroService.ContasAReceberView view) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, output);
            document.open();

            document.add(new Paragraph("Contas a Receber - FisioGestao",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
            document.add(new Paragraph(" "));

            // Resumo
            PdfPTable resumo = new PdfPTable(5);
            resumo.setWidthPercentage(100);
            resumo.addCell(cabecalho("Total a Receber"));
            resumo.addCell(cabecalho("Vencido"));
            resumo.addCell(cabecalho("A Vencer"));
            resumo.addCell(cabecalho("Títulos"));
            resumo.addCell(cabecalho("Grupos"));
            resumo.addCell(valor(moeda(view.resumo().total())));
            resumo.addCell(valor(moeda(view.resumo().totalVencido())));
            resumo.addCell(valor(moeda(view.resumo().totalAVencer())));
            resumo.addCell(valor(String.valueOf(view.resumo().quantidade())));
            resumo.addCell(valor(String.valueOf(view.resumo().gruposCount())));
            document.add(resumo);
            document.add(new Paragraph(" "));

            // Aging
            if (!view.grupos().isEmpty()) {
                document.add(new Paragraph("Aging de Recebíveis",
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
                document.add(new Paragraph(" "));
                document.add(tabelaAging(view));
            }

            document.close();
            return new RelatorioDocumento("contas-a-receber.pdf", "application/pdf", output.toByteArray());
        } catch (Exception ex) {
            throw new BusinessException("Não foi possível gerar o PDF de contas a receber.");
        }
    }

    private PdfPTable tabelaAging(FinanceiroService.ContasAReceberView view) {
        PdfPTable table = new PdfPTable(9);
        table.setWidthPercentage(100);
        float[] widths = {3f, 1.6f, 1.6f, 1.6f, 1.6f, 1.6f, 1.6f, 1.8f, 1.4f};
        try { table.setWidths(widths); } catch (Exception ignored) {}

        table.addCell(cabecalho("Paciente / Plano"));
        table.addCell(cabecalho("Mais antigo"));
        table.addCell(cabecalho("A vencer"));
        table.addCell(cabecalho("1-7 dias"));
        table.addCell(cabecalho("8-15 dias"));
        table.addCell(cabecalho("16-30 dias"));
        table.addCell(cabecalho("+30 dias"));
        table.addCell(cabecalho("Total"));
        table.addCell(cabecalho("Status"));

        for (FinanceiroService.ContaReceberGrupo g : view.grupos()) {
            String pacientePlano = g.paciente() + (g.plano() != null ? "\n" + g.plano() : "");
            table.addCell(valor(pacientePlano));
            table.addCell(valor(formatarData(g.vencimentoMaisAntigo())));
            table.addCell(valor(moeda(g.aVencer())));
            table.addCell(valor(moeda(g.vencidoAte7())));
            table.addCell(valor(moeda(g.vencido8A15())));
            table.addCell(valor(moeda(g.vencido16A30())));
            table.addCell(valor(moeda(g.vencidoAcima30())));
            table.addCell(valor(moeda(g.total())));
            String status = g.maiorAtrasoDias() > 0 ? g.maiorAtrasoDias() + " dia(s)" : "A vencer";
            table.addCell(valor(status));
        }
        return table;
    }

    // ── Excel ─────────────────────────────────────────────────────────────────

    private RelatorioDocumento gerarExcel(FinanceiroService.ContasAReceberView view) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            // Aba Resumo
            Sheet resumoSheet = workbook.createSheet("Resumo");
            Row r0 = resumoSheet.createRow(0);
            r0.createCell(0).setCellValue("Total a Receber");
            r0.createCell(1).setCellValue(view.resumo().total().doubleValue());
            Row r1 = resumoSheet.createRow(1);
            r1.createCell(0).setCellValue("Vencido");
            r1.createCell(1).setCellValue(view.resumo().totalVencido().doubleValue());
            Row r2 = resumoSheet.createRow(2);
            r2.createCell(0).setCellValue("A Vencer");
            r2.createCell(1).setCellValue(view.resumo().totalAVencer().doubleValue());
            Row r3 = resumoSheet.createRow(3);
            r3.createCell(0).setCellValue("Títulos");
            r3.createCell(1).setCellValue(view.resumo().quantidade());
            Row r4 = resumoSheet.createRow(4);
            r4.createCell(0).setCellValue("Grupos");
            r4.createCell(1).setCellValue(view.resumo().gruposCount());

            // Aba Aging
            Sheet agingSheet = workbook.createSheet("Aging de Recebíveis");
            Row header = agingSheet.createRow(0);
            header.createCell(0).setCellValue("Paciente");
            header.createCell(1).setCellValue("Plano");
            header.createCell(2).setCellValue("Mais Antigo");
            header.createCell(3).setCellValue("A Vencer (R$)");
            header.createCell(4).setCellValue("1-7 Dias (R$)");
            header.createCell(5).setCellValue("8-15 Dias (R$)");
            header.createCell(6).setCellValue("16-30 Dias (R$)");
            header.createCell(7).setCellValue("+30 Dias (R$)");
            header.createCell(8).setCellValue("Total (R$)");
            header.createCell(9).setCellValue("Maior Atraso (dias)");

            int idx = 1;
            for (FinanceiroService.ContaReceberGrupo g : view.grupos()) {
                Row row = agingSheet.createRow(idx++);
                row.createCell(0).setCellValue(g.paciente());
                row.createCell(1).setCellValue(g.plano() != null ? g.plano() : "Sem plano");
                row.createCell(2).setCellValue(formatarData(g.vencimentoMaisAntigo()));
                row.createCell(3).setCellValue(g.aVencer().doubleValue());
                row.createCell(4).setCellValue(g.vencidoAte7().doubleValue());
                row.createCell(5).setCellValue(g.vencido8A15().doubleValue());
                row.createCell(6).setCellValue(g.vencido16A30().doubleValue());
                row.createCell(7).setCellValue(g.vencidoAcima30().doubleValue());
                row.createCell(8).setCellValue(g.total().doubleValue());
                row.createCell(9).setCellValue(g.maiorAtrasoDias());
            }

            workbook.write(output);
            return new RelatorioDocumento(
                    "contas-a-receber.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    output.toByteArray()
            );
        } catch (Exception ex) {
            throw new BusinessException("Não foi possível gerar a planilha de contas a receber.");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private PdfPCell cabecalho(String texto) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8)));
        cell.setPadding(5f);
        return cell;
    }

    private PdfPCell valor(String texto) {
        PdfPCell cell = new PdfPCell(new Phrase(texto == null ? "-" : texto, FontFactory.getFont(FontFactory.HELVETICA, 8)));
        cell.setPadding(5f);
        return cell;
    }

    private String moeda(BigDecimal valor) {
        return NumberFormat.getCurrencyInstance(PT_BR).format(valor == null ? BigDecimal.ZERO : valor);
    }

    private String formatarData(LocalDate data) {
        return data != null ? data.format(FMT_DATA) : "-";
    }
}
