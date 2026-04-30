package com.fisiogestao.pacientes;

import com.fisiogestao.pacientes.service.PacienteService;
import com.fisiogestao.shared.web.BusinessException;
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
public class PacientesApiController {

    private final PacienteService pacienteService;

    @GetMapping("/por-cpf")
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
