package com.fisiogestao.empresa;

import java.util.UUID;

/** Empresa criada pela migração para receber os dados existentes antes do multiempresa. */
public final class EmpresaLegada {

    public static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final String SLUG = "clinica-principal";
    public static final String NOME = "Clínica Principal";

    private EmpresaLegada() {
    }
}
