package com.fisiogestao.auditoria.form;

import com.fisiogestao.auditoria.AuditActionType;
import com.fisiogestao.auditoria.AuditEventOrigin;
import com.fisiogestao.auditoria.AuditModule;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class AuditFilterForm {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataInicial;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataFinal;

    private AuditModule modulo;

    private String entidade;

    private AuditActionType tipoAcao;

    private String usuario;

    private String paciente;

    private String profissional;

    private String registroAfetado;

    private AuditEventOrigin origemEvento;

    private boolean somenteExclusoes;

    private boolean somenteCancelamentos;

    private boolean somenteEdicoes;

    private boolean somenteAlteracoesCriticas;
}
