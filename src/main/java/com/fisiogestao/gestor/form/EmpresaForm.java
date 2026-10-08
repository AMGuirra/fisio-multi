package com.fisiogestao.gestor.form;

import com.fisiogestao.empresa.EmpresaEntity;
import com.fisiogestao.empresa.EmpresaStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class EmpresaForm {

    @NotBlank(message = "Informe o nome da clínica.")
    @Size(max = 160)
    private String nome;

    @Size(max = 60)
    private String slug;

    @Size(max = 160)
    private String nomeResponsavel;

    @Size(max = 18)
    private String cpf;

    @Size(max = 18)
    private String cnpj;

    @Size(max = 160)
    private String email;

    @Size(max = 20)
    private String telefone;

    private EmpresaStatus status = EmpresaStatus.TESTE;

    @Size(max = 1000)
    private String motivoBloqueio;

    @Min(value = 1, message = "Informe ao menos 1 dia útil.")
    @Max(value = 3650, message = "Máximo de 3650 dias úteis.")
    private Integer quantidadeDiasUteisTeste;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataInicioTeste;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataFimTeste;

    /** Versão otimista exibida no formulário de edição. */
    private Long versao;

    public static EmpresaForm de(EmpresaEntity empresa, java.time.ZoneId zona) {
        EmpresaForm form = new EmpresaForm();
        form.setNome(empresa.getNome());
        form.setSlug(empresa.getSlug());
        form.setNomeResponsavel(empresa.getNomeResponsavel());
        form.setCpf(empresa.getCpf());
        form.setCnpj(empresa.getCnpj());
        form.setEmail(empresa.getEmail());
        form.setTelefone(empresa.getTelefone());
        form.setStatus(empresa.getStatus());
        form.setMotivoBloqueio(empresa.getMotivoBloqueio());
        form.setQuantidadeDiasUteisTeste(empresa.getQuantidadeDiasUteisTeste());
        if (empresa.getDataInicioTeste() != null) {
            form.setDataInicioTeste(empresa.getDataInicioTeste().atZoneSameInstant(zona).toLocalDate());
        }
        if (empresa.getDataFimTeste() != null) {
            form.setDataFimTeste(empresa.getDataFimTeste().atZoneSameInstant(zona).toLocalDate());
        }
        form.setVersao(empresa.getVersao());
        return form;
    }
}
