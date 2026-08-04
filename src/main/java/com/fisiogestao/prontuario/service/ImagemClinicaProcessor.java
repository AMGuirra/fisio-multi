package com.fisiogestao.prontuario.service;

import com.fisiogestao.shared.web.BusinessException;
import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * Valida a assinatura binária (magic bytes) de imagens clínicas e remove metadados
 * sensíveis (EXIF/GPS) antes da persistência.
 *
 * <p>O projeto não depende de bibliotecas de detecção de conteúdo (ex.: Apache Tika),
 * então a verificação de assinatura é feita manualmente para os três formatos aceitos
 * como fotografia clínica: JPEG, PNG e WebP.</p>
 */
@Component
public class ImagemClinicaProcessor {

    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] RIFF_MAGIC = {0x52, 0x49, 0x46, 0x46};
    private static final byte[] WEBP_MAGIC = {0x57, 0x45, 0x42, 0x50};

    /**
     * Confirma que os bytes recebidos realmente começam com a assinatura binária esperada
     * para o tipo MIME declarado. Não confia apenas em extensão/Content-Type do multipart.
     */
    public void validarAssinatura(byte[] dados, String tipoMimeDeclarado) {
        if (dados == null || dados.length < 12) {
            throw new BusinessException("Arquivo de imagem inválido ou corrompido.");
        }
        boolean assinaturaValida = switch (tipoMimeDeclarado) {
            case "image/jpeg" -> startsWith(dados, JPEG_MAGIC);
            case "image/png" -> startsWith(dados, PNG_MAGIC);
            case "image/webp" -> startsWith(dados, RIFF_MAGIC) && matchesAt(dados, 8, WEBP_MAGIC);
            default -> false;
        };
        if (!assinaturaValida) {
            throw new BusinessException(
                    "O conteúdo do arquivo não corresponde a uma imagem JPEG, PNG ou WebP válida.");
        }
    }

    /**
     * Remove metadados EXIF/GPS re-codificando os pixels da imagem (JPEG e PNG).
     * Para WebP, a plataforma Java não possui decodificador nativo — o arquivo é
     * mantido como veio, apenas com a assinatura já validada. Essa limitação é
     * documentada como pendência conhecida.
     */
    public byte[] higienizar(byte[] dadosOriginais, String tipoMime) {
        if (!"image/jpeg".equals(tipoMime) && !"image/png".equals(tipoMime)) {
            return dadosOriginais;
        }
        try {
            BufferedImage imagem = ImageIO.read(new ByteArrayInputStream(dadosOriginais));
            if (imagem == null) {
                throw new BusinessException("Não foi possível processar a imagem enviada.");
            }
            BufferedImage semAlfaEstranho = new BufferedImage(
                    imagem.getWidth(), imagem.getHeight(),
                    "image/png".equals(tipoMime) ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = semAlfaEstranho.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(imagem, 0, 0, null);
            graphics.dispose();

            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            String formato = "image/png".equals(tipoMime) ? "png" : "jpeg";
            if ("jpeg".equals(formato)) {
                escreverJpegComQualidade(semAlfaEstranho, saida, 0.9f);
            } else {
                ImageIO.write(semAlfaEstranho, formato, saida);
            }
            return saida.toByteArray();
        } catch (IOException ex) {
            throw new BusinessException("Não foi possível processar a imagem enviada.");
        }
    }

    /** Gera uma miniatura JPEG (proporção preservada, lado maior limitado) para exibição no histórico. */
    public byte[] gerarMiniatura(byte[] dadosOriginais, int ladoMaximo) {
        try {
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(dadosOriginais));
            if (original == null) {
                throw new BusinessException("Não foi possível gerar a miniatura da imagem.");
            }
            int largura = original.getWidth();
            int altura = original.getHeight();
            double escala = Math.min(1.0, (double) ladoMaximo / Math.max(largura, altura));
            int novaLargura = Math.max(1, (int) Math.round(largura * escala));
            int novaAltura = Math.max(1, (int) Math.round(altura * escala));

            BufferedImage miniatura = new BufferedImage(novaLargura, novaAltura, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = miniatura.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(original, 0, 0, novaLargura, novaAltura, null);
            graphics.dispose();

            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            escreverJpegComQualidade(miniatura, saida, 0.75f);
            return saida.toByteArray();
        } catch (IOException ex) {
            throw new BusinessException("Não foi possível gerar a miniatura da imagem.");
        }
    }

    private void escreverJpegComQualidade(BufferedImage imagem, ByteArrayOutputStream saida, float qualidade) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            ImageIO.write(imagem, "jpeg", saida);
            return;
        }
        ImageWriter writer = writers.next();
        ImageWriteParam params = writer.getDefaultWriteParam();
        params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        params.setCompressionQuality(qualidade);
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(saida)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(imagem, null, null), params);
        } finally {
            writer.dispose();
        }
    }

    private boolean startsWith(byte[] dados, byte[] assinatura) {
        return matchesAt(dados, 0, assinatura);
    }

    private boolean matchesAt(byte[] dados, int offset, byte[] assinatura) {
        if (dados.length < offset + assinatura.length) {
            return false;
        }
        for (int i = 0; i < assinatura.length; i++) {
            if (dados[offset + i] != assinatura[i]) {
                return false;
            }
        }
        return true;
    }
}
