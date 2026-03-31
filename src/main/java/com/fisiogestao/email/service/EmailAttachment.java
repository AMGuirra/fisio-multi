package com.fisiogestao.email.service;

public record EmailAttachment(
        String fileName,
        String contentType,
        byte[] content
) {
}
