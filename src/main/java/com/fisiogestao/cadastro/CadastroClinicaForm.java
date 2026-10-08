package com.fisiogestao.cadastro;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CadastroClinicaForm {

    @NotBlank(message = "Informe o nome da clínica ou do fisioterapeuta.")
    @Size(max = 160, message = "Use no máximo 160 caracteres.")
    private String nomeClinica;

    @NotBlank(message = "Informe o nome do responsável.")
    @Size(max = 160, message = "Use no máximo 160 caracteres.")
    private String nomeResponsavel;

    @Size(max = 18)
    private String cpf;

    @Size(max = 18)
    private String cnpj;

    @NotBlank(message = "Informe o e-mail.")
    @Size(max = 160)
    private String email;

    @NotBlank(message = "Informe o telefone.")
    @Size(max = 20)
    private String telefone;

    @NotBlank(message = "Informe a senha inicial.")
    @Size(max = 72)
    private String senha;

    @NotBlank(message = "Confirme a senha.")
    @Size(max = 72)
    private String confirmacaoSenha;

    @AssertTrue(message = "É necessário aceitar os termos de uso e a política de privacidade.")
    private boolean aceiteTermos;

    @Size(max = 500, message = "As observações devem ter no máximo 500 caracteres.")
    private String observacoes;
}
