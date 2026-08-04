package com.fisiogestao.prontuario;

/**
 * Distingue a natureza de um anexo em {@code prontuario_anexo}.
 */
public enum TipoAnexoProntuario {

    /** Documento clínico genérico: laudo, radiografia, avaliação em PDF/DOCX etc. */
    DOCUMENTO,

    /** Fotografia de acompanhamento clínico vinculada a uma evolução. */
    FOTO_CLINICA
}
