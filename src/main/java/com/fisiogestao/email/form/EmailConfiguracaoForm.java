package com.fisiogestao.email.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailConfiguracaoForm {

    private String id;

    @NotBlank(message = "Informe o nome do remetente.")
    @Size(max = 120, message = "O nome do remetente deve ter até 120 caracteres.")
    private String nomeRemetente;

    @NotBlank(message = "Informe o e-mail do remetente.")
    @Email(message = "Informe um e-mail válido.")
    @Size(max = 160, message = "O e-mail do remetente deve ter até 160 caracteres.")
    private String emailRemetente;

    @Size(max = 120, message = "A senha de app deve ter até 120 caracteres.")
    private String senhaApp;

    private boolean manterSenhaAtual = true;

    @NotBlank(message = "Informe o host SMTP.")
    @Size(max = 180, message = "O host SMTP deve ter até 180 caracteres.")
    private String smtpHost;

    @NotNull(message = "Informe a porta SMTP.")
    @Min(value = 1, message = "Informe uma porta SMTP válida.")
    @Max(value = 65535, message = "Informe uma porta SMTP válida.")
    private Integer smtpPort;

    private boolean usaAutenticacao = true;

    private boolean usaTls = true;

    private boolean usaSsl;

    @Size(max = 160, message = "O reply-to deve ter até 160 caracteres.")
    private String replyTo;

    private boolean ativo = true;

    @Size(max = 1000, message = "A observação deve ter até 1000 caracteres.")
    private String observacoes;
}
