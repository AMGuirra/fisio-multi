package com.fisiogestao.pacientes.service;

import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.pacientes.form.PacienteForm;
import com.fisiogestao.pacientes.repository.ClienteRepository;
import com.fisiogestao.shared.web.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PacienteService {

    private final ClienteRepository clienteRepository;
    private final JdbcTemplate jdbcTemplate;
    private volatile boolean esquemaClienteValidado = false;

    public List<ClienteEntity> listarAtivos() {
        return clienteRepository.findByAtivoTrueOrderByNomeCompletoAsc();
    }

    public ClienteEntity buscarPorId(UUID id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Paciente não encontrado."));
    }

    @Transactional
    public ClienteEntity cadastrar(PacienteForm form) {
        validarSchemaClienteId();

        ClienteEntity cliente = ClienteEntity.builder()
                .nomeCompleto(form.getNomeCompleto().trim())
                .telefone(normalizarTelefone(form.getTelefone()))
                .email(normalizarEmail(form.getEmail()))
                .dataNascimento(form.getDataNascimento())
                .observacoes(normalizarTexto(form.getObservacoes()))
                .ativo(true)
                .build();
        return clienteRepository.save(cliente);
    }

    @Transactional
    public ClienteEntity localizarOuCriar(String nomePaciente, String telefone, String email) {
        validarSchemaClienteId();

        String telefoneNormalizado = normalizarTelefone(telefone);
        String emailNormalizado = normalizarEmail(email);

        Optional<ClienteEntity> existente = Optional.empty();
        if (telefoneNormalizado != null) {
            existente = clienteRepository.findFirstByTelefone(telefoneNormalizado);
        }
        if (existente.isEmpty() && emailNormalizado != null) {
            existente = clienteRepository.findFirstByEmailIgnoreCase(emailNormalizado);
        }

        if (existente.isPresent()) {
            ClienteEntity cliente = existente.get();
            if (cliente.getTelefone() == null && telefoneNormalizado != null) {
                cliente.setTelefone(telefoneNormalizado);
            }
            if (cliente.getEmail() == null && emailNormalizado != null) {
                cliente.setEmail(emailNormalizado);
            }
            if (cliente.getNomeCompleto() == null || cliente.getNomeCompleto().isBlank()) {
                cliente.setNomeCompleto(nomePaciente.trim());
            }
            cliente.setAtivo(true);
            return clienteRepository.save(cliente);
        }

        ClienteEntity novo = ClienteEntity.builder()
                .nomeCompleto(nomePaciente.trim())
                .telefone(telefoneNormalizado)
                .email(emailNormalizado)
                .ativo(true)
                .build();
        return clienteRepository.save(novo);
    }

    private String normalizarTelefone(String telefone) {
        if (telefone == null) {
            return null;
        }
        String digits = telefone.replaceAll("\\D", "");
        return digits.isBlank() ? null : digits;
    }

    private String normalizarEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase();
    }

    private String normalizarTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim();
    }

    private void validarSchemaClienteId() {
        if (esquemaClienteValidado) {
            return;
        }

        String tipo;
        try {
            tipo = jdbcTemplate.queryForObject(
                    """
                    SELECT data_type
                    FROM information_schema.columns
                    WHERE table_schema = 'public'
                      AND table_name = 'cliente'
                      AND column_name = 'id'
                    """,
                    String.class
            );
        } catch (Exception ex) {
            throw new BusinessException("Não foi possível validar o schema da tabela cliente. Verifique a conexão e a estrutura do banco.");
        }

        if (tipo == null || tipo.isBlank()) {
            throw new BusinessException("Tabela cliente não encontrada no schema public. Verifique a base configurada para a aplicação.");
        }

        if (!"uuid".equalsIgnoreCase(tipo)) {
            throw new BusinessException(
                    "Banco incompatível: a coluna cliente.id está como " + tipo
                            + ". Ajuste o schema para UUID (script sugerido em docs/sql/correcao_cliente_id_uuid.sql)."
            );
        }

        esquemaClienteValidado = true;
    }
}
