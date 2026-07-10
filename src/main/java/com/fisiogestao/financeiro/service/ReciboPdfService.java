package com.fisiogestao.financeiro.service;

import com.fisiogestao.agenda.ConfiguracaoClinicaEntity;
import com.fisiogestao.agenda.repository.ConfiguracaoClinicaRepository;
import com.fisiogestao.config.ClinicTimezoneHolder;
import com.fisiogestao.financeiro.LancamentoFinanceiroEntity;
import com.fisiogestao.financeiro.StatusLancamentoFinanceiro;
import com.fisiogestao.financeiro.repository.LancamentoFinanceiroRepository;
import com.fisiogestao.shared.web.BusinessException;

import java.util.UUID;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ReciboPdfService {

    private static final DateTimeFormatter FMT_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final Color COR_PRIMARIA  = new Color(37, 99, 235);
    private static final Color COR_TEXTO     = new Color(15, 23, 42);
    private static final Color COR_SUBTEXTO  = new Color(100, 116, 139);
    private static final Color COR_BORDA     = new Color(226, 232, 240);
    private static final Color COR_FUNDO_ALT = new Color(248, 250, 252);

    private final LancamentoFinanceiroRepository lancamentoFinanceiroRepository;
    private final ConfiguracaoClinicaRepository configuracaoClinicaRepository;

    @Transactional(readOnly = true)
    public byte[] gerarRecibo(UUID lancamentoId) {
        LancamentoFinanceiroEntity lancamento = lancamentoFinanceiroRepository.findById(lancamentoId)
                .orElseThrow(() -> new BusinessException("Lançamento financeiro não encontrado."));

        if (lancamento.getStatus() != StatusLancamentoFinanceiro.PAGO) {
            throw new BusinessException("Recibo disponível apenas para lançamentos pagos.");
        }

        ConfiguracaoClinicaEntity clinica = configuracaoClinicaRepository
                .findFirstByAtivoTrueOrderByCreatedAtAsc()
                .orElse(null);

        ZoneId zone = clinica != null ? ZoneId.of(clinica.getTimezoneId()) : ClinicTimezoneHolder.zoneId();
        String nomeClinica = clinica != null ? clinica.getNomeClinica() : "Clínica";

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter writer = PdfWriter.getInstance(doc, baos);
            doc.open();

            BaseFont baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            BaseFont boldFont  = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);

            Font fontTitulo    = new Font(boldFont, 20, Font.NORMAL, COR_PRIMARIA);
            Font fontSubtitulo = new Font(baseFont, 10, Font.NORMAL, COR_SUBTEXTO);
            Font fontLabel     = new Font(boldFont, 9, Font.NORMAL, COR_SUBTEXTO);
            Font fontValor     = new Font(baseFont, 10, Font.NORMAL, COR_TEXTO);
            Font fontValorGrande = new Font(boldFont, 18, Font.NORMAL, COR_PRIMARIA);
            Font fontRodape    = new Font(baseFont, 8, Font.NORMAL, COR_SUBTEXTO);

            // === Cabeçalho ===
            PdfPTable header = new PdfPTable(2);
            header.setWidthPercentage(100);
            header.setWidths(new float[]{2f, 1f});
            header.setSpacingAfter(24);

            PdfPCell celClinica = new PdfPCell();
            celClinica.setBorder(Rectangle.NO_BORDER);
            celClinica.setPaddingBottom(4);
            celClinica.addElement(new Paragraph(nomeClinica, fontTitulo));
            if (clinica != null && clinica.getEmailContato() != null) {
                celClinica.addElement(new Paragraph(clinica.getEmailContato(), fontSubtitulo));
            }
            if (clinica != null && clinica.getTelefoneWhatsapp() != null) {
                celClinica.addElement(new Paragraph(clinica.getTelefoneWhatsapp(), fontSubtitulo));
            }
            header.addCell(celClinica);

            PdfPCell celRecibo = new PdfPCell();
            celRecibo.setBorder(Rectangle.NO_BORDER);
            celRecibo.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Font fontReciboTitulo = new Font(boldFont, 14, Font.NORMAL, COR_PRIMARIA);
            Paragraph pRecibo = new Paragraph("RECIBO", fontReciboTitulo);
            pRecibo.setAlignment(Element.ALIGN_RIGHT);
            celRecibo.addElement(pRecibo);
            String idCurto = lancamento.getId().toString().substring(0, 8).toUpperCase();
            Paragraph pNum = new Paragraph("#" + idCurto, fontSubtitulo);
            pNum.setAlignment(Element.ALIGN_RIGHT);
            celRecibo.addElement(pNum);
            header.addCell(celRecibo);

            doc.add(header);

            // Linha divisória
            adicionarLinha(doc, writer, COR_PRIMARIA);
            doc.add(Chunk.NEWLINE);

            // === Endereço da clínica ===
            if (clinica != null) {
                String endereco = montarEndereco(clinica);
                if (!endereco.isBlank()) {
                    Paragraph pEndereco = new Paragraph(endereco, fontSubtitulo);
                    pEndereco.setSpacingAfter(16);
                    doc.add(pEndereco);
                }
            }

            // === Dados do pagamento ===
            PdfPTable dados = new PdfPTable(2);
            dados.setWidthPercentage(100);
            dados.setWidths(new float[]{1f, 1f});
            dados.setSpacingAfter(20);

            adicionarCampo(dados, "RECEBEMOS DE", lancamento.getCliente() != null
                    ? lancamento.getCliente().getNomeCompleto() : "—", fontLabel, fontValor, true);
            adicionarCampo(dados, "DATA DO PAGAMENTO", lancamento.getDataPagamento() != null
                    ? lancamento.getDataPagamento().atZoneSameInstant(zone).format(FMT_DATA) : "—",
                    fontLabel, fontValor, false);
            adicionarCampo(dados, "DESCRIÇÃO", lancamento.getDescricao(), fontLabel, fontValor, true);
            adicionarCampo(dados, "CATEGORIA", lancamento.getCategoria(), fontLabel, fontValor, false);
            adicionarCampo(dados, "FORMA DE PAGAMENTO",
                    lancamento.getMetodoPagamento() != null ? lancamento.getMetodoPagamento().getLabel() : "—",
                    fontLabel, fontValor, true);
            adicionarCampo(dados, "DATA DE COMPETÊNCIA",
                    lancamento.getDataRecebimento().atZoneSameInstant(zone).format(FMT_DATA),
                    fontLabel, fontValor, false);
            doc.add(dados);

            // === Valor em destaque ===
            PdfPTable tabelaValor = new PdfPTable(1);
            tabelaValor.setWidthPercentage(100);
            tabelaValor.setSpacingAfter(32);

            PdfPCell celValor = new PdfPCell();
            celValor.setBackgroundColor(COR_FUNDO_ALT);
            celValor.setBorderColor(COR_BORDA);
            celValor.setBorderWidth(1f);
            celValor.setPadding(16);

            Paragraph pLabel = new Paragraph("VALOR TOTAL RECEBIDO", fontLabel);
            pLabel.setSpacingAfter(4);
            celValor.addElement(pLabel);

            String valorFormatado = "R$ " + String.format(Locale.US, "%,.2f", lancamento.getValor())
                    .replace(',', 'X').replace('.', ',').replace('X', '.');
            Paragraph pValor = new Paragraph(valorFormatado, fontValorGrande);
            celValor.addElement(pValor);
            tabelaValor.addCell(celValor);
            doc.add(tabelaValor);

            // Linha divisória final
            adicionarLinha(doc, writer, COR_BORDA);
            doc.add(Chunk.NEWLINE);

            // === Assinatura ===
            PdfPTable assinatura = new PdfPTable(2);
            assinatura.setWidthPercentage(100);
            assinatura.setWidths(new float[]{1f, 1f});
            assinatura.setSpacingAfter(8);

            PdfPCell celAss1 = new PdfPCell();
            celAss1.setBorder(Rectangle.NO_BORDER);
            celAss1.setPaddingTop(24);
            celAss1.addElement(new Paragraph("_______________________________", fontSubtitulo));
            celAss1.addElement(new Paragraph(nomeClinica, new Font(boldFont, 9, Font.NORMAL, COR_TEXTO)));
            celAss1.addElement(new Paragraph("Prestador de Serviço", fontSubtitulo));
            assinatura.addCell(celAss1);

            PdfPCell celAss2 = new PdfPCell();
            celAss2.setBorder(Rectangle.NO_BORDER);
            celAss2.setPaddingTop(24);
            celAss2.setHorizontalAlignment(Element.ALIGN_RIGHT);
            celAss2.addElement(new Paragraph("_______________________________", fontSubtitulo));
            celAss2.addElement(new Paragraph(
                    lancamento.getCliente() != null ? lancamento.getCliente().getNomeCompleto() : "Paciente",
                    new Font(boldFont, 9, Font.NORMAL, COR_TEXTO)));
            celAss2.addElement(new Paragraph("Tomador de Serviço", fontSubtitulo));
            assinatura.addCell(celAss2);
            doc.add(assinatura);

            // === Rodapé ===
            String dataGeracao = ClinicTimezoneHolder.now().atZoneSameInstant(zone)
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm"));
            Paragraph rodape = new Paragraph("Documento gerado em " + dataGeracao + " pelo sistema FisioGestão.", fontRodape);
            rodape.setAlignment(Element.ALIGN_CENTER);
            rodape.setSpacingBefore(8);
            doc.add(rodape);

            doc.close();
            return baos.toByteArray();

        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("Erro ao gerar recibo PDF: " + ex.getMessage(), ex);
        }
    }

    private void adicionarCampo(PdfPTable tabela, String label, String valor,
                                 Font fontLabel, Font fontValor, boolean fundoAlternado) {
        PdfPCell cel = new PdfPCell();
        cel.setBorderColor(COR_BORDA);
        cel.setBorderWidth(0.5f);
        cel.setPadding(10);
        if (fundoAlternado) {
            cel.setBackgroundColor(COR_FUNDO_ALT);
        }
        Paragraph pLabel = new Paragraph(label, fontLabel);
        pLabel.setSpacingAfter(2);
        cel.addElement(pLabel);
        cel.addElement(new Paragraph(valor != null ? valor : "—", fontValor));
        tabela.addCell(cel);
    }

    private void adicionarLinha(Document doc, PdfWriter writer, Color cor) throws DocumentException {
        PdfContentByte cb = writer.getDirectContent();
        cb.setColorStroke(cor);
        cb.setLineWidth(1f);
        float y = writer.getVerticalPosition(false);
        cb.moveTo(doc.leftMargin(), y);
        cb.lineTo(doc.right(), y);
        cb.stroke();
        doc.add(new Paragraph(" "));
    }

    private String montarEndereco(ConfiguracaoClinicaEntity clinica) {
        StringBuilder sb = new StringBuilder();
        if (clinica.getLogradouro() != null) sb.append(clinica.getLogradouro());
        if (clinica.getNumero() != null) sb.append(", ").append(clinica.getNumero());
        if (clinica.getComplemento() != null && !clinica.getComplemento().isBlank())
            sb.append(" - ").append(clinica.getComplemento());
        if (clinica.getBairro() != null) sb.append(", ").append(clinica.getBairro());
        if (clinica.getCidade() != null) {
            sb.append(" — ").append(clinica.getCidade());
            if (clinica.getEstado() != null) sb.append("/").append(clinica.getEstado());
        }
        if (clinica.getCep() != null) sb.append(" — CEP ").append(clinica.getCep());
        return sb.toString();
    }
}
