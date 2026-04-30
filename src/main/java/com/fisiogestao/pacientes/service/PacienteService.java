package com.fisiogestao.pacientes.service;

import com.fisiogestao.agenda.repository.SessaoRepository;
import com.fisiogestao.auditoria.AuditActionType;
import com.fisiogestao.auditoria.AuditEventOrigin;
import com.fisiogestao.auditoria.AuditModule;
import com.fisiogestao.auditoria.service.AuditService;
import com.fisiogestao.financeiro.repository.LancamentoFinanceiroRepository;
import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.pacientes.form.PacienteForm;
import com.fisiogestao.pacientes.repository.ClienteRepository;
import com.fisiogestao.pacientes.repository.PacoteSessaoRepository;
import com.fisiogestao.prontuario.repository.ProntuarioSoapRepository;
import com.fisiogestao.shared.web.BusinessException;
import com.fisiogestao.solicitacoes.repository.SolicitacaoAgendamentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("null")
public class PacienteService {

    private final ClienteRepository clienteRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PacoteSessaoRepository pacoteSessaoRepository;
    private final SessaoRepository sessaoRepository;
    private final ProntuarioSoapRepository prontuarioSoapRepository;
    private final SolicitacaoAgendamentoRepository solicitacaoAgendamentoRepository;
    private final LancamentoFinanceiroRepository lancamentoFinanceiroRepository;
    private final AuditService auditService;
    private volatile boolean esquemaClienteValidado = false;
    private volatile boolean relacionamentoSolicitacaoClienteValidado = false;

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
        String cpfNormalizado = normalizarCpf(form.getCpf());
        if (cpfNormalizado != null && clienteRepository.findFirstByCpf(cpfNormalizado).isPresent()) {
            throw new BusinessException("Já existe um paciente cadastrado com este CPF.");
        }

        ClienteEntity cliente = ClienteEntity.builder()
                .nomeCompleto(normalizarNome(form.getNomeCompleto()))
                .telefone(normalizarTelefone(form.getTelefone()))
                .email(normalizarEmail(form.getEmail()))
                .cpf(cpfNormalizado)
                .dataNascimento(form.getDataNascimento())
                .cep(normalizarCep(form.getCep()))
                .rua(normalizarTexto(form.getRua()))
                .numero(normalizarTexto(form.getNumero()))
                .bairro(normalizarTexto(form.getBairro()))
                .cidade(normalizarTexto(form.getCidade()))
                .estado(normalizarEstado(form.getEstado()))
                .observacoes(normalizarTexto(form.getObservacoes()))
                .ativo(true)
                .build();
        ClienteEntity salvo = clienteRepository.save(cliente);
        auditService.recordCreate(
                AuditModule.PACIENTES,
                "cliente",
                salvo,
                null,
                "Cadastro manual de paciente.",
                AuditEventOrigin.MANUAL
        );
        return salvo;
    }

    public Optional<ClienteEntity> buscarPorCpf(String cpf) {
        validarSchemaClienteId();
        String cpfNormalizado = normalizarCpf(cpf);
        if (cpfNormalizado == null) {
            return Optional.empty();
        }
        return clienteRepository.findFirstByCpf(cpfNormalizado);
    }

    public Optional<ClienteEntity> buscarPorCpfTelefoneOuEmail(String cpf, String telefone, String email) {
        validarSchemaClienteId();

        String cpfNormalizado = normalizarCpf(cpf);
        if (cpfNormalizado != null) {
            Optional<ClienteEntity> porCpf = clienteRepository.findFirstByCpf(cpfNormalizado);
            if (porCpf.isPresent()) {
                return porCpf;
            }
        }

        String telefoneNormalizado = normalizarTelefone(telefone);
        if (telefoneNormalizado != null) {
            Optional<ClienteEntity> porTelefone = clienteRepository.findFirstByTelefone(telefoneNormalizado);
            if (porTelefone.isPresent()) {
                return porTelefone;
            }
        }

        String emailNormalizado = normalizarEmail(email);
        if (emailNormalizado != null) {
            return clienteRepository.findFirstByEmailIgnoreCase(emailNormalizado);
        }

        return Optional.empty();
    }

    public void validarRelacionamentoSolicitacaoClienteUuid() {
        validarSchemaClienteId();
        if (relacionamentoSolicitacaoClienteValidado) {
            return;
        }
        validarColunaUuid(
                "solicitacao_agendamento",
                "cliente_id",
                "Banco incompatível: a coluna solicitacao_agendamento.cliente_id está como %s. "
                        + "Ajuste o schema para UUID (script sugerido em docs/sql/correcao_cliente_id_uuid.sql)."
        );
        relacionamentoSolicitacaoClienteValidado = true;
    }

    @Transactional
    public ClienteEntity localizarOuCriar(String nomePaciente, String telefone, String email) {
        PacienteForm form = new PacienteForm();
        form.setNomeCompleto(nomePaciente);
        form.setTelefone(telefone);
        form.setEmail(email);
        return localizarOuCriar(
                form,
                AuditEventOrigin.ONLINE,
                "Atualização automática durante processamento de solicitação online.",
                "Paciente criado automaticamente a partir de solicitação online."
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ClienteEntity localizarOuCriar(
            PacienteForm form,
            AuditEventOrigin origin,
            String mensagemAtualizacao,
            String mensagemCriacao
    ) {
        validarSchemaClienteId();
        Optional<ClienteEntity> existente = buscarPorCpfTelefoneOuEmail(form.getCpf(), form.getTelefone(), form.getEmail());

        if (existente.isPresent()) {
            ClienteEntity cliente = existente.get();
            var snapshotAnterior = auditService.snapshotOf(cliente);
            aplicarDados(cliente, form, false);
            cliente.setAtivo(true);
            try {
                ClienteEntity salvo = clienteRepository.save(cliente);
                registrarAuditoriaAtualizacaoSemFalhar(salvo, snapshotAnterior, mensagemAtualizacao, origin);
                return salvo;
            } catch (DataIntegrityViolationException ex) {
                return reconciliarClienteAposConflito(form, snapshotAnterior, mensagemAtualizacao, mensagemCriacao, origin, ex);
            }
        }

        ClienteEntity novo = ClienteEntity.builder().ativo(true).build();
        aplicarDados(novo, form, true);
        try {
            ClienteEntity salvo = clienteRepository.save(novo);
            registrarAuditoriaCriacaoSemFalhar(salvo, mensagemCriacao, origin);
            return salvo;
        } catch (DataIntegrityViolationException ex) {
            return reconciliarClienteAposConflito(form, null, mensagemAtualizacao, mensagemCriacao, origin, ex);
        }
    }

    @Transactional
    public void excluir(UUID clienteId) {
        ClienteEntity cliente = buscarPorId(clienteId);
        var snapshotAnterior = auditService.snapshotOf(cliente);

        if (pacoteSessaoRepository.existsByClienteId(clienteId)) {
            throw new BusinessException("Exclua primeiro os planos de tratamento vinculados a este paciente.");
        }
        if (sessaoRepository.existsByClienteId(clienteId)) {
            throw new BusinessException("Este paciente possui histórico de sessões e não pode ser excluído.");
        }
        if (prontuarioSoapRepository.existsByClienteId(clienteId)) {
            throw new BusinessException("Este paciente possui prontuário registrado e não pode ser excluído.");
        }
        if (solicitacaoAgendamentoRepository.existsByClienteId(clienteId)) {
            throw new BusinessException("Este paciente possui histórico de solicitações e não pode ser excluído.");
        }
        if (lancamentoFinanceiroRepository.existsByClienteId(clienteId)) {
            throw new BusinessException("Este paciente possui movimentação financeira vinculada e não pode ser excluído.");
        }

        auditService.recordManual(
                AuditModule.PACIENTES,
                "cliente",
                cliente.getId().toString(),
                cliente.getNomeCompleto(),
                AuditActionType.DELETE_PHYSICAL,
                snapshotAnterior,
                null,
                null,
                "Exclusão física de paciente.",
                AuditEventOrigin.MANUAL
        );
        clienteRepository.delete(cliente);
    }

    private String normalizarTelefone(String telefone) {
        if (telefone == null) {
            return null;
        }
        String digits = telefone.replaceAll("\\D", "");
        return digits.isBlank() ? null : digits;
    }

    private String normalizarCpf(String cpf) {
        if (cpf == null) {
            return null;
        }
        String digits = cpf.replaceAll("\\D", "");
        if (digits.isBlank()) {
            return null;
        }
        if (digits.length() != 11) {
            throw new BusinessException("Informe um CPF válido com 11 dígitos.");
        }
        return digits;
    }

    private String normalizarCep(String cep) {
        if (cep == null) {
            return null;
        }
        String digits = cep.replaceAll("\\D", "");
        if (digits.isBlank()) {
            return null;
        }
        if (digits.length() != 8) {
            throw new BusinessException("Informe um CEP válido com 8 dígitos.");
        }
        return digits;
    }

    private String normalizarEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase();
    }

    private String normalizarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new BusinessException("Informe o nome do paciente.");
        }
        return nome.trim();
    }

    private String normalizarEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return null;
        }
        String valor = estado.trim().toUpperCase();
        if (valor.length() != 2) {
            throw new BusinessException("Selecione um estado válido.");
        }
        return valor;
    }

    private String normalizarTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim();
    }

    private void aplicarDados(ClienteEntity cliente, PacienteForm form, boolean sobrescreverNome) {
        String nomeNormalizado = normalizarNome(form.getNomeCompleto());
        String telefoneNormalizado = normalizarTelefone(form.getTelefone());
        String emailNormalizado = normalizarEmail(form.getEmail());
        String cpfNormalizado = normalizarCpf(form.getCpf());
        String cepNormalizado = normalizarCep(form.getCep());
        String ruaNormalizada = normalizarTexto(form.getRua());
        String numeroNormalizado = normalizarTexto(form.getNumero());
        String bairroNormalizado = normalizarTexto(form.getBairro());
        String cidadeNormalizada = normalizarTexto(form.getCidade());
        String estadoNormalizado = normalizarEstado(form.getEstado());
        String observacoesNormalizadas = normalizarTexto(form.getObservacoes());

        if (sobrescreverNome || nomeNormalizado != null) {
            cliente.setNomeCompleto(nomeNormalizado);
        }
        if (telefoneNormalizado != null) {
            cliente.setTelefone(telefoneNormalizado);
        }
        if (emailNormalizado != null) {
            cliente.setEmail(emailNormalizado);
        }
        if (cpfNormalizado != null) {
            cliente.setCpf(cpfNormalizado);
        }
        if (form.getDataNascimento() != null) {
            cliente.setDataNascimento(form.getDataNascimento());
        }
        if (cepNormalizado != null) {
            cliente.setCep(cepNormalizado);
        }
        if (ruaNormalizada != null) {
            cliente.setRua(ruaNormalizada);
        }
        if (numeroNormalizado != null) {
            cliente.setNumero(numeroNormalizado);
        }
        if (bairroNormalizado != null) {
            cliente.setBairro(bairroNormalizado);
        }
        if (cidadeNormalizada != null) {
            cliente.setCidade(cidadeNormalizada);
        }
        if (estadoNormalizado != null) {
            cliente.setEstado(estadoNormalizado);
        }
        if (observacoesNormalizadas != null) {
            cliente.setObservacoes(observacoesNormalizadas);
        }
    }

    private void validarSchemaClienteId() {
        if (esquemaClienteValidado) {
            return;
        }

        String tipo = obterTipoColuna("cliente", "id");
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

    private void validarColunaUuid(String tabela, String coluna, String mensagemIncompatibilidade) {
        String tipo = obterTipoColuna(tabela, coluna);
        if (tipo == null || tipo.isBlank()) {
            throw new BusinessException(
                    "Estrutura incompatível: a coluna " + tabela + "." + coluna
                            + " não foi encontrada no schema public. Verifique as migrations aplicadas no banco."
            );
        }
        if (!"uuid".equalsIgnoreCase(tipo)) {
            throw new BusinessException(mensagemIncompatibilidade.formatted(tipo));
        }
    }

    private String obterTipoColuna(String tabela, String coluna) {
        try {
            return jdbcTemplate.queryForObject(
                    """
                    SELECT data_type
                    FROM information_schema.columns
                    WHERE table_schema = 'public'
                      AND table_name = ?
                      AND column_name = ?
                    """,
                    String.class,
                    tabela,
                    coluna
            );
        } catch (Exception ex) {
            throw new BusinessException(
                    "Não foi possível validar o schema da tabela " + tabela + ". Verifique a conexão e a estrutura do banco."
            );
        }
    }

    private ClienteEntity reconciliarClienteAposConflito(
            PacienteForm form,
            java.util.Map<String, Object> snapshotAnterior,
            String mensagemAtualizacao,
            String mensagemCriacao,
            AuditEventOrigin origin,
            DataIntegrityViolationException exception
    ) {
        Optional<ClienteEntity> conciliado = buscarPorCpfTelefoneOuEmail(form.getCpf(), form.getTelefone(), form.getEmail());
        if (conciliado.isEmpty()) {
            throw new BusinessException("Ja existe um paciente cadastrado com os dados informados. Revise CPF, telefone e e-mail.");
        }

        ClienteEntity cliente = conciliado.get();
        var snapshotBase = snapshotAnterior != null ? snapshotAnterior : auditService.snapshotOf(cliente);
        aplicarDados(cliente, form, false);
        cliente.setAtivo(true);

        try {
            ClienteEntity salvo = clienteRepository.save(cliente);
            String observacao = snapshotAnterior != null ? mensagemAtualizacao : mensagemCriacao;
            registrarAuditoriaAtualizacaoSemFalhar(salvo, snapshotBase, observacao, origin);
            log.warn("Conflito de integridade conciliado automaticamente no cadastro de paciente. clienteId={}", salvo.getId(), exception);
            return salvo;
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException("Ja existe um paciente cadastrado com os dados informados. Revise CPF, telefone e e-mail.");
        }
    }

    private void registrarAuditoriaAtualizacaoSemFalhar(
            ClienteEntity cliente,
            java.util.Map<String, Object> snapshotAnterior,
            String mensagemAtualizacao,
            AuditEventOrigin origin
    ) {
        try {
            auditService.recordManual(
                    AuditModule.PACIENTES,
                    "cliente",
                    cliente.getId().toString(),
                    cliente.getNomeCompleto(),
                    AuditActionType.UPDATE,
                    snapshotAnterior,
                    auditService.snapshotOf(cliente),
                    null,
                    mensagemAtualizacao,
                    origin
            );
        } catch (Exception ex) {
            log.warn("Falha ao registrar auditoria de atualizacao do paciente {}.", cliente.getId(), ex);
        }
    }

    private void registrarAuditoriaCriacaoSemFalhar(
            ClienteEntity cliente,
            String mensagemCriacao,
            AuditEventOrigin origin
    ) {
        try {
            auditService.recordCreate(
                    AuditModule.PACIENTES,
                    "cliente",
                    cliente,
                    null,
                    mensagemCriacao,
                    origin
            );
        } catch (Exception ex) {
            log.warn("Falha ao registrar auditoria de criacao do paciente {}.", cliente.getId(), ex);
        }
    }
}
