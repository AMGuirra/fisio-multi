package com.fisiogestao.prontuario;

/**
 * Distingue a natureza de um registro em {@code prontuario_soap}.
 */
public enum TipoRegistroProntuario {

    /** Registro clínico principal do paciente: único, editável em texto livre. */
    AVALIACAO,

    /** Entrada de diário clínico contínuo: um novo registro independente por salvamento. */
    EVOLUCAO
}
