package com.fisiogestao.pacientes.service;

import com.fisiogestao.agenda.repository.ConfiguracaoClinicaRepository;
import com.fisiogestao.agenda.repository.HorarioBloqueadoRepository;
import com.fisiogestao.agenda.repository.SessaoRepository;
import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.auth.PasswordPolicy;
import com.fisiogestao.auditoria.AuditActionType;
import com.fisiogestao.auditoria.AuditEventOrigin;
import com.fisiogestao.auditoria.AuditModule;
import com.fisiogestao.auditoria.service.AuditService;
import com.fisiogestao.config.ClinicTimezoneHolder;
import com.fisiogestao.email.EmailLogType;
import com.fisiogestao.email.service.EmailRequest;
import com.fisiogestao.email.service.SystemEmailService;
import com.fisiogestao.financeiro.repository.LancamentoFinanceiroRepository;
import com.fisiogestao.listaespera.ListaEsperaRepository;
import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.pacientes.form.PacienteForm;
import com.fisiogestao.pacientes.dto.PacienteListagemDTO;
import com.fisiogestao.pacientes.repository.ClienteRepository;
import com.fisiogestao.pacientes.repository.PlanoSessaoRepository;
import com.fisiogestao.portal.PacientePasswordResetTokenEntity;
import com.fisiogestao.portal.PacientePasswordResetTokenRepository;
import com.fisiogestao.prontuario.repository.ProntuarioAnexoRepository;
import com.fisiogestao.prontuario.repository.ProntuarioSoapRepository;
import com.fisiogestao.shared.util.DocumentoUtils;
import com.fisiogestao.shared.web.BusinessException;
import com.fisiogestao.solicitacoes.SolicitacaoAgendamentoEntity;
import com.fisiogestao.solicitacoes.repository.SolicitacaoAgendamentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PacienteService {

    private final ClienteRepository clienteRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PlanoSessaoRepository planoSessaoRepository;
    private final SessaoRepository sessaoRepository;
    private final ProntuarioSoapRepository prontuarioSoapRepository;
    private final SolicitacaoAgendamentoRepository solicitacaoAgendamentoRepository;
    private final LancamentoFinanceiroRepository lancamentoFinanceiroRepository;
    private final ProntuarioAnexoRepository prontuarioAnexoRepository;
    private final ListaEsperaRepository listaEsperaRepository;
    private final HorarioBloqueadoRepository horarioBloqueadoRepository;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;
    private final PacientePasswordResetTokenRepository pacientePasswordResetTokenRepository;
    private final SystemEmailService systemEmailService;
    private final ConfiguracaoClinicaRepository configuracaoClinicaRepository;

    @Value("${app.base-url}")
    private URI trustedBaseUri;

    private static final int RESET_TOKEN_TTL_MINUTES = 60;

    // Flags de compatibilidade de schema: validadas uma vez na inicialização pelo SchemaCompatibilityValidator.
    // Mantidas aqui como fallback para chamadas que ocorram antes do validator terminar (ex: testes).
    private volatile boolean esquemaClienteValidado = false;
    private volatile boolean relacionamentoSolicitacaoClienteValidado = false;

    private static final int TAMANHO_PAGINA = 20;

    @Transactional(readOnly = true)
    public List<ClienteEntity> listarAtivos() {
        return clienteRepository.findByAtivoTrueOrderByNomeCompletoAsc();
    }

    /**
     * Lista paginada de pacientes ativos com busca opcional por nome, CPF, telefone ou e-mail.
     *
     * @param page  índice da página (0-based)
     * @param busca termo de busca case-insensitive; nulo ou vazio retorna todos os ativos
     */
    @Transactional(readOnly = true)
    public Page<ClienteEntity> listarAtivosPaginado(int page, String busca) {
        return listarPaginado(page, busca, "ATIVOS");
    }

    @Transactional(readOnly = true)
    public Page<ClienteEntity> listarPaginado(int page, String busca, String status) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), TAMANHO_PAGINA);
        String buscaNormalizada = (busca == null || busca.isBlank()) ? null : busca.strip();
        Boolean ativo = switch (status == null ? "ATIVOS" : status) {
            case "TODOS" -> null;
            case "INATIVOS" -> false;
            default -> true;
        };
        return clienteRepository.findPaginado(buscaNormalizada, ativo, pageable);
    }

    /**
     * Complementa a página já paginada em três consultas por conjunto, evitando uma consulta
     * para cada paciente ao renderizar tratamento e sessões.
     */
    @Transactional(readOnly = true)
    public List<PacienteListagemDTO> montarListagem(Page<ClienteEntity> pagina) {
        List<ClienteEntity> clientes = pagina.getContent();
        if (clientes.isEmpty()) {
            return List.of();
        }

        List<UUID> ids = clientes.stream().map(ClienteEntity::getId).toList();
        Map<UUID, com.fisiogestao.pacientes.PlanoSessaoEntity> planos = new HashMap<>();
        planoSessaoRepository.findByClienteIdInAndStatusOrderByClienteIdAscCreatedAtDesc(ids, com.fisiogestao.pacientes.PlanoSessaoStatus.ATIVO)
                .forEach(plano -> planos.putIfAbsent(plano.getCliente().getId(), plano));

        Map<UUID, SessaoEntity> ultimas = new HashMap<>();
        sessaoRepository.findUltimasRealizadasPorClientes(ids)
                .forEach(sessao -> ultimas.putIfAbsent(sessao.getCliente().getId(), sessao));
        Map<UUID, SessaoEntity> proximas = new HashMap<>();
        sessaoRepository.findProximasAgendadasPorClientes(ids, ClinicTimezoneHolder.now())
                .forEach(sessao -> proximas.putIfAbsent(sessao.getCliente().getId(), sessao));

        return clientes.stream()
                .map(cliente -> new PacienteListagemDTO(
                        cliente,
                        planos.get(cliente.getId()),
                        ultimas.get(cliente.getId()),
                        proximas.get(cliente.getId()),
                        DocumentoUtils.formatarCpf(cliente.getCpf()),
                        DocumentoUtils.formatarTelefone(cliente.getTelefone())
                ))
                .toList();
    }

    /** Conta pacientes ativos. Usa índice do banco para evitar full scan. */
    @Transactional(readOnly = true)
    public long contarAtivos() {
        return clienteRepository.countByAtivoTrue();
    }

    @Transactional(readOnly = true)
    public long contarAtivosCriadosNoPeriodo(OffsetDateTime inicioInclusive, OffsetDateTime fimExclusivo) {
        return clienteRepository.countByAtivoTrueAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                inicioInclusive,
                fimExclusivo
        );
    }

    @Transactional(readOnly = true)
    public List<ClienteEntity> listarAtivosRecentes() {
        return clienteRepository.findTop6ByAtivoTrueOrderByCreatedAtDesc();
    }

    /** Conta pacientes ativos que possuem telefone preenchido. */
    @Transactional(readOnly = true)
    public long contarAtivosComTelefone() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cliente WHERE ativo = true AND telefone IS NOT NULL AND telefone <> ''",
                Long.class);
        return count != null ? count : 0L;
    }

    /** Conta pacientes ativos que possuem e-mail preenchido. */
    @Transactional(readOnly = true)
    public long contarAtivosComEmail() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cliente WHERE ativo = true AND email IS NOT NULL AND email <> ''",
                Long.class);
        return count != null ? count : 0L;
    }

    @Transactional(readOnly = true)
    public long contarAtivosSemEmail() {
        return Math.max(0, contarAtivos() - contarAtivosComEmail());
    }

    @Transactional(readOnly = true)
    public long contarAtivosComPlanoAtivo() {
        return planoSessaoRepository.countPacientesAtivosComPlanoAtivo();
    }

    @Transactional(readOnly = true)
    public long contarSessoesHoje() {
        LocalDate hoje = ClinicTimezoneHolder.today();
        OffsetDateTime inicio = hoje.atStartOfDay(ClinicTimezoneHolder.zoneId()).toOffsetDateTime();
        OffsetDateTime fim = hoje.plusDays(1).atStartOfDay(ClinicTimezoneHolder.zoneId()).toOffsetDateTime();
        return sessaoRepository.countNaoCanceladasNoPeriodo(inicio, fim);
    }

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
    public Optional<ClienteEntity> buscarPorCpf(String cpf) {
        validarSchemaClienteId();
        String cpfNormalizado = normalizarCpf(cpf);
        if (cpfNormalizado == null) {
            return Optional.empty();
        }
        return clienteRepository.findFirstByCpf(cpfNormalizado);
    }

    @Transactional(readOnly = true)
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

    /**
     * Chamado pelo {@code SchemaCompatibilityValidator} na inicialização para evitar
     * revalidação de schema em cada request de runtime.
     */
    public void marcarSchemaValidado() {
        this.esquemaClienteValidado = true;
        this.relacionamentoSolicitacaoClienteValidado = true;
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
    public ClienteEntity atualizar(UUID id, PacienteForm form) {
        ClienteEntity cliente = buscarPorId(id);
        var snapshotAnterior = auditService.snapshotOf(cliente);

        String cpfNormalizado = normalizarCpf(form.getCpf());
        if (cpfNormalizado != null && !cpfNormalizado.equals(cliente.getCpf())) {
            clienteRepository.findFirstByCpf(cpfNormalizado)
                    .filter(outro -> !outro.getId().equals(id))
                    .ifPresent(outro -> { throw new BusinessException("Já existe outro paciente cadastrado com este CPF."); });
        }

        cliente.setNomeCompleto(normalizarNome(form.getNomeCompleto()));
        cliente.setTelefone(normalizarTelefone(form.getTelefone()));
        cliente.setEmail(normalizarEmail(form.getEmail()));
        cliente.setCpf(cpfNormalizado);
        cliente.setDataNascimento(form.getDataNascimento());
        cliente.setCep(normalizarCep(form.getCep()));
        cliente.setRua(normalizarTexto(form.getRua()));
        cliente.setNumero(normalizarTexto(form.getNumero()));
        cliente.setBairro(normalizarTexto(form.getBairro()));
        cliente.setCidade(normalizarTexto(form.getCidade()));
        cliente.setEstado(normalizarEstado(form.getEstado()));
        cliente.setObservacoes(normalizarTexto(form.getObservacoes()));

        ClienteEntity salvo = clienteRepository.save(cliente);
        registrarAuditoriaAtualizacaoSemFalhar(salvo, snapshotAnterior, "Edição manual de dados do paciente.", AuditEventOrigin.MANUAL);
        return salvo;
    }

    /**
     * Gera um novo portalToken para o paciente, invalidando o link anterior.
     * Deve ser chamado quando o paciente reportar que seu link foi comprometido,
     * ou como ação preventiva periódica.
     *
     * @param clienteId ID do paciente
     * @return novo token gerado
     */
    @Transactional
    public UUID rotacionarPortalToken(UUID clienteId) {
        ClienteEntity cliente = buscarPorId(clienteId);
        var snapshotAnterior = auditService.snapshotOf(cliente);
        UUID novoToken = UUID.randomUUID();
        cliente.setPortalToken(novoToken);
        clienteRepository.save(cliente);
        auditService.recordManual(
                AuditModule.PACIENTES,
                "cliente",
                cliente.getId().toString(),
                cliente.getNomeCompleto(),
                AuditActionType.UPDATE,
                snapshotAnterior,
                auditService.snapshotOf(cliente),
                null,
                "Token do portal do paciente rotacionado por segurança.",
                AuditEventOrigin.MANUAL
        );
        return novoToken;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void definirSenhaPortal(UUID clienteId, String senhaRaw) {
        String senhaNormalizada = PasswordPolicy.normalize(senhaRaw, true);
        ClienteEntity cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new BusinessException("Paciente não encontrado ao definir senha."));
        cliente.setSenhaPortal(passwordEncoder.encode(senhaNormalizada));
        clienteRepository.save(cliente);
    }

    /**
     * Hash bcrypt "descartável" usado apenas para igualar o tempo de resposta quando o
     * identificador informado não corresponde a nenhum paciente ou a conta ainda não tem
     * senha definida — evita que o tempo de resposta revele se o identificador existe.
     */
    private static final String DUMMY_BCRYPT_HASH =
            "$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5L0EmZUsD3dqU/rM0lVn8DYWs8yjW";

    /** Resultado da tentativa de login do portal do paciente por CPF ou celular. */
    public enum StatusAutenticacaoPortal {
        SUCESSO,
        INVALIDO,
        /** Celular pertence a mais de um cadastro nesta clínica: login por celular fica ambíguo. */
        CELULAR_AMBIGUO
    }

    public record ResultadoAutenticacaoPortal(StatusAutenticacaoPortal status, ClienteEntity paciente) {
        static ResultadoAutenticacaoPortal sucesso(ClienteEntity paciente) {
            return new ResultadoAutenticacaoPortal(StatusAutenticacaoPortal.SUCESSO, paciente);
        }

        static ResultadoAutenticacaoPortal invalido() {
            return new ResultadoAutenticacaoPortal(StatusAutenticacaoPortal.INVALIDO, null);
        }

        static ResultadoAutenticacaoPortal celularAmbiguo() {
            return new ResultadoAutenticacaoPortal(StatusAutenticacaoPortal.CELULAR_AMBIGUO, null);
        }
    }

    /**
     * Autentica um paciente no portal por CPF ou celular (conforme {@code tipoIdentificador}) + senha.
     *
     * <p>A senha é sempre exigida: contas sem senha definida não conseguem logar — devem usar
     * "esqueci minha senha" primeiro. Não diferencia "identificador inexistente" de "senha
     * incorreta" na resposta (ambos retornam {@link StatusAutenticacaoPortal#INVALIDO}), para
     * evitar enumeração de pacientes cadastrados.</p>
     *
     * @param tipoIdentificador {@code "cpf"} ou {@code "celular"} — define qual campo é consultado.
     */
    @Transactional(readOnly = true)
    public ResultadoAutenticacaoPortal autenticarPortalPorTipo(String tipoIdentificador, String identificador, String senhaRaw) {
        if ("celular".equalsIgnoreCase(tipoIdentificador == null ? "" : tipoIdentificador.trim())) {
            return autenticarPortalPorCelular(identificador, senhaRaw);
        }
        return autenticarPortalPorCpf(identificador, senhaRaw);
    }

    /**
     * Login por CPF. Também aceita e-mail no mesmo campo por compatibilidade com contas
     * criadas antes da divisão explícita CPF/celular na tela de login — o e-mail não é mais
     * oferecido como opção visível, mas continua funcionando para quem já usava esse acesso.
     */
    private ResultadoAutenticacaoPortal autenticarPortalPorCpf(String identificador, String senhaRaw) {
        if (identificador != null && identificador.contains("@")) {
            ClienteEntity porEmail = clienteRepository.findFirstByEmailIgnoreCase(identificador.trim().toLowerCase())
                    .orElse(null);
            return validarSenhaPortal(porEmail, senhaRaw);
        }
        String cpfNormalizado = DocumentoUtils.normalizarCpf(identificador);
        ClienteEntity cliente = DocumentoUtils.cpfTemTamanhoValido(cpfNormalizado)
                ? clienteRepository.findFirstByCpf(cpfNormalizado).orElse(null)
                : null;
        return validarSenhaPortal(cliente, senhaRaw);
    }

    private ResultadoAutenticacaoPortal autenticarPortalPorCelular(String identificador, String senhaRaw) {
        String telefoneNormalizado = DocumentoUtils.normalizarTelefone(identificador);
        if (!DocumentoUtils.telefoneTemTamanhoValido(telefoneNormalizado)) {
            return validarSenhaPortal(null, senhaRaw);
        }

        List<ClienteEntity> encontrados = clienteRepository.findAllByTelefone(telefoneNormalizado);
        if (encontrados.size() > 1) {
            // Compara com um hash descartável só para não vazar, por tempo de resposta, que este
            // caminho é diferente do "identificador não encontrado".
            passwordEncoder.matches(senhaRaw == null ? "" : senhaRaw, DUMMY_BCRYPT_HASH);
            return ResultadoAutenticacaoPortal.celularAmbiguo();
        }

        ClienteEntity cliente = encontrados.isEmpty() ? null : encontrados.get(0);
        return validarSenhaPortal(cliente, senhaRaw);
    }

    private ResultadoAutenticacaoPortal validarSenhaPortal(ClienteEntity cliente, String senhaRaw) {
        boolean temSenhaDefinida = cliente != null
                && cliente.getSenhaPortal() != null
                && !cliente.getSenhaPortal().isBlank();
        String hashParaComparar = temSenhaDefinida ? cliente.getSenhaPortal() : DUMMY_BCRYPT_HASH;
        boolean senhaConfere = senhaRaw != null && !senhaRaw.isBlank()
                && passwordEncoder.matches(senhaRaw, hashParaComparar);

        if (!temSenhaDefinida || !senhaConfere || cliente == null || !Boolean.TRUE.equals(cliente.getAtivo())) {
            return ResultadoAutenticacaoPortal.invalido();
        }
        return ResultadoAutenticacaoPortal.sucesso(cliente);
    }

    /**
     * Ponto de entrada do "esqueci minha senha" do portal: aceita e-mail, CPF ou telefone
     * como identificador, já que muitos pacientes (em especial idosos) não têm e-mail cadastrado.
     *
     * <p>Quando o paciente identificado tem e-mail, segue o fluxo padrão de link de redefinição.
     * Quando não tem, a solicitação é encaminhada para o e-mail de contato da clínica
     * (Configurações da Clínica), para que a equipe ajude o paciente presencialmente ou por telefone.</p>
     */
    @Transactional
    public void solicitarAjudaSenhaPaciente(String identificador, String requestIp, String userAgent, String requestBaseUrl) {
        // Mantido por retrocompatibilidade; requestBaseUrl nunca participa de links de seguranca.
        if (identificador == null || identificador.isBlank()) {
            return;
        }
        ClienteEntity cliente = buscarPorIdentificadorAjudaSenha(identificador).orElse(null);
        if (cliente == null || !Boolean.TRUE.equals(cliente.getAtivo())) {
            return;
        }
        if (cliente.getEmail() != null && !cliente.getEmail().isBlank()) {
            solicitarResetSenhaPaciente(cliente.getEmail(), requestIp, userAgent, requestBaseUrl);
            return;
        }
        notificarClinicaSemEmailPaciente(cliente);
    }

    private Optional<ClienteEntity> buscarPorIdentificadorAjudaSenha(String identificador) {
        String valor = identificador.trim();
        if (valor.contains("@")) {
            return clienteRepository.findFirstByEmailIgnoreCase(valor.toLowerCase());
        }
        String digits = valor.replaceAll("\\D", "");
        if (digits.length() == 11) {
            Optional<ClienteEntity> porCpf = clienteRepository.findFirstByCpf(digits);
            if (porCpf.isPresent()) {
                return porCpf;
            }
        }
        if (!digits.isEmpty()) {
            return clienteRepository.findFirstByTelefone(digits);
        }
        return Optional.empty();
    }

    private void notificarClinicaSemEmailPaciente(ClienteEntity cliente) {
        configuracaoClinicaRepository.findFirstByAtivoTrueOrderByCreatedAtAsc().ifPresent(clinica -> {
            String emailDestino = clinica.getEmailContato();
            if (emailDestino == null || emailDestino.isBlank() || !emailDestino.contains("@")) {
                return;
            }
            systemEmailService.send(
                    EmailRequest.builder()
                            .type(EmailLogType.SOLICITACAO_SENHA_PACIENTE_SEM_EMAIL)
                            .subject("Paciente sem e-mail pediu ajuda com a senha do portal")
                            .to(emailDestino)
                            .templateName("email/solicitacao-senha-paciente-sem-email")
                            .templateId("solicitacao-senha-paciente-sem-email")
                            .entityType("cliente")
                            .entityId(cliente.getId())
                            .triggeredBy("sistema")
                            .templateVariables(Map.of(
                                    "nomePaciente", cliente.getNomeCompleto(),
                                    "telefonePaciente", cliente.getTelefone() != null && !cliente.getTelefone().isBlank()
                                            ? cliente.getTelefone() : "Não informado",
                                    "cpfPaciente", cliente.getCpf() != null && !cliente.getCpf().isBlank()
                                            ? cliente.getCpf() : "Não informado"
                            ))
                            .build()
            );
            log.debug("E-mail de solicitação de ajuda de senha (paciente sem e-mail) enviado para a clínica, cliente {}.", cliente.getId());
        });
    }

    @Transactional
    public void solicitarResetSenhaPaciente(String email, String requestIp, String userAgent, String requestBaseUrl) {
        if (email == null || email.isBlank()) {
            return;
        }
        clienteRepository.findFirstByEmailIgnoreCase(email.trim().toLowerCase()).ifPresent(cliente -> {
            if (!Boolean.TRUE.equals(cliente.getAtivo())) {
                return;
            }
            String rawToken = gerarTokenSeguro();
            String tokenHash = hashToken(rawToken);
            OffsetDateTime agora = ClinicTimezoneHolder.now();

            pacientePasswordResetTokenRepository.findByClienteIdAndUsedAtIsNull(cliente.getId())
                    .forEach(t -> t.setUsedAt(agora));

            PacientePasswordResetTokenEntity entity = PacientePasswordResetTokenEntity.builder()
                    .cliente(cliente)
                    .tokenHash(tokenHash)
                    .expiresAt(agora.plusMinutes(RESET_TOKEN_TTL_MINUTES))
                    .requestedIp(requestIp)
                    .requestedUserAgent(userAgent != null && userAgent.length() > 512 ? userAgent.substring(0, 512) : userAgent)
                    .build();
            pacientePasswordResetTokenRepository.save(entity);

            String link = montarLinkResetPaciente(rawToken);
            systemEmailService.send(
                    EmailRequest.builder()
                            .type(EmailLogType.RESET_SENHA_PACIENTE)
                            .subject("Recuperação de senha - Portal do Paciente")
                            .to(cliente.getEmail())
                            .templateName("email/reset-senha-paciente")
                            .templateId("reset-senha-paciente")
                            .entityType("cliente")
                            .entityId(cliente.getId())
                            .triggeredBy("sistema")
                            .templateVariables(Map.of(
                                    "nomePaciente", cliente.getNomeCompleto(),
                                    "linkRedefinicao", link,
                                    "expiraEm", entity.getExpiresAt(),
                                    "baseUrl", trustedBaseUri.toString()
                            ))
                            .build()
            );
            log.debug("E-mail de reset de senha do portal enviado para cliente {}.", cliente.getId());
        });
    }

    @Transactional
    public void redefinirSenhaPaciente(String rawToken, String novaSenha, String confirmacaoSenha) {
        String senhaNormalizada = PasswordPolicy.normalize(novaSenha, true);
        String confirmacaoNormalizada = confirmacaoSenha == null ? null : confirmacaoSenha.trim();

        if (!senhaNormalizada.equals(confirmacaoNormalizada)) {
            throw new BusinessException("A confirmação da senha não confere.");
        }

        if (rawToken == null || rawToken.isBlank()) {
            throw new BusinessException("O link de redefinição é inválido.");
        }

        String hash = hashToken(rawToken);
        PacientePasswordResetTokenEntity token = pacientePasswordResetTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BusinessException("O link de redefinição é inválido ou já expirou."));

        if (!token.ativoEm(ClinicTimezoneHolder.now())) {
            throw new BusinessException("O link de redefinição é inválido ou já expirou.");
        }

        ClienteEntity cliente = token.getCliente();
        cliente.setSenhaPortal(passwordEncoder.encode(senhaNormalizada));
        OffsetDateTime redefinicao = ClinicTimezoneHolder.now();
        token.setUsedAt(redefinicao);
        clienteRepository.save(cliente);
        pacientePasswordResetTokenRepository.save(token);

        auditService.recordManual(
                AuditModule.PACIENTES,
                "cliente",
                cliente.getId().toString(),
                cliente.getNomeCompleto(),
                AuditActionType.RESET_PASSWORD,
                Map.of("senhaRedefinida", false),
                Map.of("senhaRedefinida", true, "senhaRedefinidaEm", redefinicao),
                null,
                "Senha do portal redefinida pelo fluxo de recuperação.",
                AuditEventOrigin.ONLINE
        );
    }

    @Transactional(readOnly = true)
    public boolean tokenResetPacienteValido(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return false;
        try {
            String hash = hashToken(rawToken);
            return pacientePasswordResetTokenRepository.findByTokenHash(hash)
                    .map(t -> t.ativoEm(ClinicTimezoneHolder.now()))
                    .orElse(false);
        } catch (Exception ex) {
            return false;
        }
    }

    private String gerarTokenSeguro() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new BusinessException("Não foi possível validar o token de redefinição.");
        }
    }

    private String montarLinkResetPaciente(String rawToken) {
        URI base = validarBaseConfiavel(trustedBaseUri);
        return base.resolve("/paciente/resetar-senha?token=" + rawToken).toString();
    }

    private URI validarBaseConfiavel(URI candidate) {
        if (candidate == null || !candidate.isAbsolute() || candidate.getHost() == null
                || candidate.getUserInfo() != null || candidate.getQuery() != null || candidate.getFragment() != null
                || !("http".equalsIgnoreCase(candidate.getScheme()) || "https".equalsIgnoreCase(candidate.getScheme()))) {
            throw new IllegalStateException("app.base-url deve ser uma URL HTTP(S) absoluta e confiavel.");
        }
        return candidate;
    }

    /**
     * Exclusão em cascata: remove todos os registros vinculados ao paciente (anexos de prontuário,
     * prontuário, lançamentos financeiros, sessões, planos de tratamento, bloqueios de agenda oriundos
     * de solicitações, solicitações de agendamento, lista de espera e tokens de reset de senha) antes de
     * excluir o cadastro. A ordem respeita as chaves estrangeiras (sem ON DELETE CASCADE no banco).
     */
    @Transactional
    public void excluir(UUID clienteId) {
        ClienteEntity cliente = buscarPorId(clienteId);
        var snapshotAnterior = auditService.snapshotOf(cliente);

        prontuarioAnexoRepository.deleteByClienteId(clienteId);
        prontuarioSoapRepository.deleteByClienteId(clienteId);
        lancamentoFinanceiroRepository.deleteByClienteId(clienteId);
        sessaoRepository.deleteByClienteId(clienteId);

        planoSessaoRepository.limparPlanoAnteriorPorCliente(clienteId);
        planoSessaoRepository.deleteByClienteId(clienteId);

        List<UUID> solicitacaoIds = solicitacaoAgendamentoRepository
                .findByClienteIdOrderByCreatedAtDesc(clienteId).stream()
                .map(SolicitacaoAgendamentoEntity::getId)
                .toList();
        if (!solicitacaoIds.isEmpty()) {
            horarioBloqueadoRepository.deleteBySolicitacaoIdIn(solicitacaoIds);
        }
        solicitacaoAgendamentoRepository.deleteByClienteId(clienteId);

        listaEsperaRepository.deleteByClienteId(clienteId);
        pacientePasswordResetTokenRepository.deleteByClienteId(clienteId);

        auditService.recordManual(
                AuditModule.PACIENTES,
                "cliente",
                cliente.getId().toString(),
                cliente.getNomeCompleto(),
                AuditActionType.DELETE_PHYSICAL,
                snapshotAnterior,
                null,
                null,
                "Exclusão física de paciente e de todos os registros vinculados (cascata).",
                AuditEventOrigin.MANUAL
        );
        clienteRepository.delete(cliente);
    }

    private String normalizarTelefone(String telefone) {
        return DocumentoUtils.normalizarTelefone(telefone);
    }

    private String normalizarCpf(String cpf) {
        String digits = DocumentoUtils.normalizarCpf(cpf);
        if (digits == null) {
            return null;
        }
        if (!DocumentoUtils.cpfTemTamanhoValido(digits)) {
            throw new BusinessException("Informe um CPF válido com 11 dígitos.");
        }
        return digits;
    }

    private String normalizarCep(String cep) {
        String digits = DocumentoUtils.somenteDigitos(cep);
        if (digits == null) {
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
