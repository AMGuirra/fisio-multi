package com.fisiogestao.prontuario.form;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Nova entrada do diário clínico contínuo (Evolução), com fotos de acompanhamento opcionais.
 */
@Getter
@Setter
public class EvolucaoForm {

    @NotNull(message = "Selecione o paciente.")
    private UUID clienteId;

    private UUID sessaoId;

    @Size(max = 8000, message = "A evolução deve ter no máximo 8000 caracteres.")
    private String texto;

    private List<MultipartFile> fotos;

    @Size(max = 120, message = "Região corporal deve ter no máximo 120 caracteres.")
    private String regiaoFotos;

    @Size(max = 1000, message = "Observação das fotos deve ter no máximo 1000 caracteres.")
    private String observacaoFotos;

    /** Confirmação explícita de que o paciente autorizou o registro fotográfico. */
    private boolean consentimentoFotos;
}
