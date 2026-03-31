package com.fisiogestao.relatorios.service;

public record RelatorioDocumento(
        String fileName,
        String contentType,
        byte[] content
) {
}
