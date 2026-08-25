package com.fisiogestao.prontuario.service;

import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.agenda.repository.SessaoRepository;
import com.fisiogestao.auditoria.AuditActionType;
import com.fisiogestao.auditoria.AuditEventOrigin;
import com.fisiogestao.auditoria.AuditModule;
import com.fisiogestao.auditoria.service.AuditService;
import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.pacientes.service.PacienteService;
import com.fisiogestao.prontuario.ProntuarioSoapEntity;
import com.fisiogestao.prontuario.TipoRegistroProntuario;
import com.fisiogestao.prontuario.dto.HistoricoClinicoItemDto;
import com.fisiogestao.prontuario.dto.ProntuarioAnexoResumoDto;
import com.fisiogestao.prontuario.dto.TipoHistoricoClinico;
import com.fisiogestao.prontuario.form.AvaliacaoForm;
import com.fisiogestao.prontuario.form.EvolucaoEditForm;
import com.fisiogestao.prontuario.form.EvolucaoForm;
import com.fisiogestao.prontuario.repository.ProntuarioSoapRepository;
import com.fisiogestao.shared.web.BusinessException;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class ProntuarioService {

    /**
     * Nome do índice único parcial criado na V50 (uma AVALIACAO por cliente). Usado para
     * distinguir, dentro do catch de {@link #salvarAvaliacao}, a corrida de duplo
     * salvamento de qualquer outra violação de integridade — que não deve ser mascarada
     * com a mesma mensagem amigável.
     */
    private static final String INDICE_UNICO_AVALIACAO_POR_CLIENTE = "uq_prontuario_soap_avaliacao_por_cliente";

    private final ProntuarioSoapRepository prontuarioSoapRepository;
    private final PacienteService pacienteService;
    private final SessaoRepository sessaoRepository;
    private final AuditService auditService;
    private final ProntuarioAnexoService prontuarioAnexoService;

    /** Todos os registros (avaliação + evoluções + SOAP legado) de um paciente, do mais recente ao mais antigo. */
    public List<ProntuarioSoapEntity> listarPorPaciente(UUID clienteId) {
        return prontuarioSoapRepository.findByClienteIdOrderByCreatedAtDesc(clienteId);
    }

    /** Histórico de evoluções (diário clínico) do paciente, do mais recente ao mais antigo. */
    public List<ProntuarioSoapEntity> listarEvolucoes(UUID clienteId) {
        return prontuarioSoapRepository.findByClienteIdAndTipoRegistroOrderByCreatedAtDesc(
                clienteId, TipoRegistroProntuario.EVOLUCAO);
    }

    /** Avaliação clínica principal do paciente, se já existir. */
    public Optional<ProntuarioSoapEntity> buscarAvaliacao(UUID clienteId) {
        return prontuarioSoapRepository.findFirstByClienteIdAndTipoRegistro(
                clienteId, TipoRegistroProntuario.AVALIACAO);
    }

    /**
     * Cria ou atualiza a Avaliação principal do paciente.
     * Atualizações são auditadas com o valor anterior e o novo (retificação, nunca sobrescrita silenciosa).
     */
    @Transactional
    public ProntuarioSoapEntity salvarAvaliacao(AvaliacaoForm form, String usuario) {
        if (!avaliacaoPreenchida(form)) {
            throw new BusinessException("A avaliação não pode ficar vazia.");
        }

        ClienteEntity cliente = pacienteService.buscarPorId(form.getClienteId());
        Optional<ProntuarioSoapEntity> existente = buscarAvaliacao(cliente.getId());

        if (existente.isPresent()) {
            ProntuarioSoapEntity avaliacao = existente.get();
            Map<String, Object> snapshotAnterior = auditService.snapshotOf(avaliacao);
            aplicarAvaliacaoEstruturada(avaliacao, form);
            ProntuarioSoapEntity salvo = prontuarioSoapRepository.save(avaliacao);
            auditService.recordUpdate(
                    AuditModule.PRONTUARIO,
                    "prontuario_soap",
                    salvo.getId().toString(),
                    "Avaliação de " + cliente.getNomeCompleto() + " atualizada.",
                    snapshotAnterior,
                    auditService.snapshotOf(salvo),
                    null,
                    "Avaliação clínica retificada pelo usuário " + usuario + ".",
                    AuditEventOrigin.MANUAL
            );
            return salvo;
        }

        ProntuarioSoapEntity avaliacao = ProntuarioSoapEntity.builder()
                .cliente(cliente)
                .tipoRegistro(TipoRegistroProntuario.AVALIACAO)
                .queixaPrincipal(texto(form.getQueixaPrincipal()))
                .avaliacaoFisica(texto(form.getAvaliacaoFisica()))
                .regiaoCorporal(texto(form.getRegiaoCorporal()))
                .avaliacaoFuncional(texto(form.getAvaliacaoFuncional()))
                .objetivosTratamento(texto(form.getObjetivosTratamento()))
                .planoTerapeutico(texto(form.getPlanoTerapeutico()))
                .observacoesAvaliacao(texto(form.getObservacoesAvaliacao()))
                .criadoPor(usuario)
                .build();
        ProntuarioSoapEntity salvo;
        try {
            // saveAndFlush (não save) é essencial aqui: save() apenas registra a entidade no
            // contexto de persistência e pode adiar o INSERT até o commit da transação, fora
            // deste try/catch. O flush força a execução do INSERT agora, dentro do escopo em
            // que a violação de constraint pode ser capturada e traduzida.
            salvo = prontuarioSoapRepository.saveAndFlush(avaliacao);
        } catch (DataIntegrityViolationException ex) {
            if (!violaIndiceUnicoDeAvaliacao(ex)) {
                throw ex;
            }
            // Duplo clique / duplo envio: duas requisições concorrentes viram "sem avaliação
            // existente" antes de qualquer uma commitar e tentam inserir ao mesmo tempo,
            // colidindo com o índice único parcial uq_prontuario_soap_avaliacao_por_cliente
            // (V50). Sem este catch, a exceção do banco subiria crua como 500.
            throw new BusinessException(
                    "Esta avaliação já foi salva em outra requisição. Atualize a página e confira o conteúdo antes de salvar novamente.");
        }
        auditService.recordCreate(
                AuditModule.PRONTUARIO,
                "prontuario_soap",
                salvo,
                null,
                "Avaliação criada no prontuário de " + cliente.getNomeCompleto() + ".",
                AuditEventOrigin.MANUAL
        );
        return salvo;
    }

    /** Monta a linha cronológica única usada pela aba Histórico Clínico. */
    public List<HistoricoClinicoItemDto> montarHistoricoClinico(
            ProntuarioSoapEntity avaliacao,
            List<ProntuarioSoapEntity> evolucoes,
            List<ProntuarioAnexoResumoDto> anexos
    ) {
        List<HistoricoClinicoItemDto> itens = new ArrayList<>();
        if (avaliacao != null) {
            itens.add(new HistoricoClinicoItemDto(avaliacao.getId(), TipoHistoricoClinico.AVALIACAO,
                    avaliacao.getUpdatedAt(), "Avaliação inicial",
                    resumoAvaliacao(avaliacao), avaliacao.getCriadoPor(), "clipboard-list"));
        }
        for (ProntuarioSoapEntity evolucao : evolucoes) {
            itens.add(new HistoricoClinicoItemDto(evolucao.getId(), TipoHistoricoClinico.EVOLUCAO,
                    evolucao.getCreatedAt(), "Evolução clínica", texto(evolucao.getTextoRegistro()),
                    evolucao.getCriadoPor(), "notebook-pen"));
        }
        for (ProntuarioAnexoResumoDto anexo : anexos) {
            TipoHistoricoClinico tipo = anexo.isFotoClinica() ? TipoHistoricoClinico.FOTO : TipoHistoricoClinico.DOCUMENTO;
            itens.add(new HistoricoClinicoItemDto(anexo.getId(), tipo, anexo.getCreatedAt(), anexo.getNomeOriginal(),
                    texto(anexo.getDescricao()), anexo.getCriadoPor(), anexo.isFotoClinica() ? "image" : anexo.getIcone()));
        }
        return itens.stream().sorted(Comparator.comparing(HistoricoClinicoItemDto::dataHora,
                Comparator.nullsLast(Comparator.reverseOrder()))).toList();
    }

    private boolean avaliacaoPreenchida(AvaliacaoForm form) {
        return java.util.stream.Stream.of(form.getQueixaPrincipal(), form.getAvaliacaoFisica(), form.getRegiaoCorporal(),
                        form.getAvaliacaoFuncional(), form.getObjetivosTratamento(), form.getPlanoTerapeutico(),
                        form.getObservacoesAvaliacao())
                .anyMatch(valor -> valor != null && !valor.isBlank());
    }

    private void aplicarAvaliacaoEstruturada(ProntuarioSoapEntity avaliacao, AvaliacaoForm form) {
        // textoRegistro é o histórico imutável de avaliações antigas; nunca é alterado aqui.
        avaliacao.setQueixaPrincipal(texto(form.getQueixaPrincipal()));
        avaliacao.setAvaliacaoFisica(texto(form.getAvaliacaoFisica()));
        avaliacao.setRegiaoCorporal(texto(form.getRegiaoCorporal()));
        avaliacao.setAvaliacaoFuncional(texto(form.getAvaliacaoFuncional()));
        avaliacao.setObjetivosTratamento(texto(form.getObjetivosTratamento()));
        avaliacao.setPlanoTerapeutico(texto(form.getPlanoTerapeutico()));
        avaliacao.setObservacoesAvaliacao(texto(form.getObservacoesAvaliacao()));
    }

    private String resumoAvaliacao(ProntuarioSoapEntity avaliacao) {
        String resumo = texto(avaliacao.getQueixaPrincipal());
        return resumo != null ? resumo : texto(avaliacao.getTextoRegistro());
    }

    private String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    /**
     * Confirma que a violação veio especificamente do índice único de avaliação por cliente,
     * e não de outra constraint (ex.: FK de cliente inválido). Evita mascarar um erro de
     * integridade diferente com a mensagem de "duplo salvamento".
     *
     * <p>O nome de constraint reportado pelo driver/dialeto vem qualificado com o schema
     * (confirmado em teste contra H2: {@code "public.uq_prontuario_soap_avaliacao_por_cliente"}),
     * por isso a comparação usa {@code endsWith} sobre o nome sem qualificação, em vez de
     * igualdade exata — uma comparação exata teria falhado silenciosamente e deixado a
     * exceção crua subir mesmo no caso que este catch existe para tratar.
     */
    private boolean violaIndiceUnicoDeAvaliacao(DataIntegrityViolationException ex) {
        Throwable causa = ex;
        while (causa != null) {
            if (causa instanceof ConstraintViolationException constraintViolation) {
                String nomeConstraint = constraintViolation.getConstraintName();
                return nomeConstraint != null
                        && nomeConstraint.toLowerCase(Locale.ROOT).endsWith(INDICE_UNICO_AVALIACAO_POR_CLIENTE);
            }
            causa = causa.getCause();
        }
        return false;
    }

    /**
     * Registra uma nova entrada no diário clínico (Evolução) — sempre um registro independente,
     * nunca uma sobrescrita. Fotos de acompanhamento, quando enviadas, são persistidas na mesma
     * transação: se qualquer foto falhar a validação, nada é gravado.
     */
    @Transactional
    public ProntuarioSoapEntity registrarEvolucao(EvolucaoForm form, String usuario) {
        String texto = form.getTexto() != null ? form.getTexto().trim() : "";
        List<MultipartFile> fotos = form.getFotos() == null
                ? List.of()
                : form.getFotos().stream().filter(f -> f != null && !f.isEmpty()).toList();

        if (texto.isEmpty() && fotos.isEmpty()) {
            throw new BusinessException("Escreva a evolução ou anexe ao menos uma foto do acompanhamento.");
        }
        if (texto.isEmpty() && !fotos.isEmpty()) {
            if (form.getRegiaoFotos() == null || form.getRegiaoFotos().isBlank()
                    || form.getObservacaoFotos() == null || form.getObservacaoFotos().isBlank()) {
                throw new BusinessException(
                        "Evoluções compostas somente por fotos exigem região corporal e observação clínica.");
            }
        }
        if (!fotos.isEmpty() && !form.isConsentimentoFotos()) {
            throw new BusinessException(
                    "Confirme que o paciente autorizou o registro fotográfico antes de enviar as fotos.");
        }

        ClienteEntity cliente = pacienteService.buscarPorId(form.getClienteId());

        SessaoEntity sessao = null;
        if (form.getSessaoId() != null) {
            sessao = sessaoRepository.findById(form.getSessaoId())
                    .orElseThrow(() -> new BusinessException("Sessão vinculada à evolução não encontrada."));
            if (!sessao.getCliente().getId().equals(cliente.getId())) {
                throw new BusinessException("A sessão informada não pertence ao paciente selecionado.");
            }
        }

        ProntuarioSoapEntity evolucao = ProntuarioSoapEntity.builder()
                .cliente(cliente)
                .sessao(sessao)
                .tipoRegistro(TipoRegistroProntuario.EVOLUCAO)
                .textoRegistro(texto.isEmpty() ? null : texto)
                .criadoPor(usuario)
                .build();
        ProntuarioSoapEntity salvo = prontuarioSoapRepository.save(Objects.requireNonNull(evolucao));

        auditService.recordCreate(
                AuditModule.PRONTUARIO,
                "prontuario_soap",
                salvo,
                null,
                "Evolução registrada no prontuário de " + cliente.getNomeCompleto() + ".",
                AuditEventOrigin.MANUAL
        );

        if (!fotos.isEmpty()) {
            prontuarioAnexoService.salvarFotosClinicas(
                    cliente.getId(),
                    salvo.getId(),
                    fotos,
                    form.getRegiaoFotos(),
                    form.getObservacaoFotos(),
                    usuario,
                    form.isConsentimentoFotos()
            );
        }

        return salvo;
    }

    /**
     * Edita o texto de uma entrada já registrada no diário clínico. A auditoria grava o
     * valor anterior e o novo, como retificação — nunca uma sobrescrita silenciosa.
     */
    @Transactional
    public ProntuarioSoapEntity atualizarEvolucao(UUID id, EvolucaoEditForm form, String usuario) {
        String texto = form.getTexto() != null ? form.getTexto().trim() : "";
        if (texto.isEmpty()) {
            throw new BusinessException("A evolução não pode ficar vazia.");
        }

        ProntuarioSoapEntity evolucao = buscarEvolucao(id, form.getClienteId());

        Map<String, Object> snapshotAnterior = auditService.snapshotOf(evolucao);
        evolucao.setTextoRegistro(texto);
        ProntuarioSoapEntity salvo = prontuarioSoapRepository.save(evolucao);

        auditService.recordUpdate(
                AuditModule.PRONTUARIO,
                "prontuario_soap",
                salvo.getId().toString(),
                "Evolução de " + salvo.getCliente().getNomeCompleto() + " editada.",
                snapshotAnterior,
                auditService.snapshotOf(salvo),
                null,
                "Evolução retificada pelo usuário " + usuario + ".",
                AuditEventOrigin.MANUAL
        );

        return salvo;
    }

    /**
     * Exclui permanentemente uma entrada do diário clínico e registra o evento na auditoria.
     */
    @Transactional
    public void excluirEvolucao(UUID id, UUID clienteId, String usuario) {
        ProntuarioSoapEntity evolucao = buscarEvolucao(id, clienteId);

        Map<String, Object> snapshotAnterior = auditService.snapshotOf(evolucao);
        String clienteNome = evolucao.getCliente().getNomeCompleto();

        prontuarioSoapRepository.delete(evolucao);

        auditService.recordAction(
                AuditModule.PRONTUARIO,
                "prontuario_soap",
                id.toString(),
                "Evolução de " + clienteNome + " excluída.",
                AuditActionType.DELETE_PHYSICAL,
                snapshotAnterior,
                null,
                null,
                "Exclusão de evolução pelo usuário " + usuario + ".",
                AuditEventOrigin.MANUAL
        );
    }

    private ProntuarioSoapEntity buscarEvolucao(UUID id, UUID clienteId) {
        ProntuarioSoapEntity evolucao = prontuarioSoapRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Registro de evolução não encontrado."));

        if (evolucao.getTipoRegistro() != TipoRegistroProntuario.EVOLUCAO) {
            throw new BusinessException("Registro de evolução não encontrado.");
        }
        if (!evolucao.getCliente().getId().equals(clienteId)) {
            throw new BusinessException("A evolução informada não pertence ao paciente selecionado.");
        }

        return evolucao;
    }
}
