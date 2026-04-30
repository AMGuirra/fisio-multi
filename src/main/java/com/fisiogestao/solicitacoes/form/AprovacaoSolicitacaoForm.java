package com.fisiogestao.solicitacoes.form;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class AprovacaoSolicitacaoForm {

    private UUID pacoteSessaoId;

    private String nomeCompleto;

    private String telefone;

    private String email;

    private String cpf;

    private String cep;

    private String rua;

    private String numero;

    private String bairro;

    private String cidade;

    private String estado;
}
