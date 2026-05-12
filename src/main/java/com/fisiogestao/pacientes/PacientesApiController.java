package com.fisiogestao.pacientes;

import com.fisiogestao.pacientes.service.PacienteService;
import com.fisiogestao.shared.web.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/pacientes")
@RequiredArgsConstructor
@Tag(name = "Pacientes", description = "Consulta de dados de pacientes cadastrados")
@SecurityRequirement(name = "basicAuth")
public class PacientesApiController {

    private final PacienteService pacienteService;

    @GetMapping("/por-cpf")
    @Operation(
            summary = "Buscar paciente por CPF",
            description = """
                    Consulta o cadastro de um paciente pelo CPF (apenas dígitos ou formatado com pontuação).
                    Retorna `encontrado: false` quando não há correspondência — nunca lança 404,
                    para facilitar integrações de agendamento online.
                    """,
            parameters = {
                    @Parameter(name = "cpf", description = "CPF do paciente (com ou sem pontuação)", example = "12345678900", required = true)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Consulta realizada — `encontrado` indica se o cadastro existe",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = PacientesApiController.PacientePorCpfResponse.class))),
                    @ApiResponse(responseCode = "400", description = "CPF inválido (ex: menos de 11 dígitos)"),
                    @ApiResponse(responseCode = "401", description = "Não autenticado")
            }
    )
    public PacientePorCpfResponse buscarPorCpf(@RequestParam String cpf) {
        try {
            return pacienteService.buscarPorCpf(cpf)
                    .map(cliente -> new PacientePorCpfResponse(
                            true,
                            cliente.getNomeCompleto(),
                            cliente.getTelefone(),
                            cliente.getEmail(),
                            cliente.getCpf(),
                            cliente.getCep(),
                            cliente.getRua(),
                            cliente.getNumero(),
                            cliente.getBairro(),
                            cliente.getCidade(),
                            cliente.getEstado()
                    ))
                    .orElseGet(() -> new PacientePorCpfResponse(false, null, null, null, null, null, null, null, null, null, null));
        } catch (BusinessException ex) {
            throw new ResponseStatusException(BAD_REQUEST, ex.getMessage());
        }
    }

    public record PacientePorCpfResponse(
            boolean encontrado,
            String nomeCompleto,
            String telefone,
            String email,
            String cpf,
            String cep,
            String rua,
            String numero,
            String bairro,
            String cidade,
            String estado
    ) {}
}
