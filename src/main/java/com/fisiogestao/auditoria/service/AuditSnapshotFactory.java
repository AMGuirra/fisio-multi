package com.fisiogestao.auditoria.service;

import com.fisiogestao.agenda.BloqueioAgendaEntity;
import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.auth.InMemoryUserCatalog;
import com.fisiogestao.auth.UsuarioSistemaEntity;
import com.fisiogestao.email.EmailConfiguracaoEntity;
import com.fisiogestao.financeiro.LancamentoFinanceiroEntity;
import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.pacientes.PlanoSessaoEntity;
import com.fisiogestao.prontuario.ProntuarioAnexoEntity;
import com.fisiogestao.prontuario.ProntuarioSoapEntity;
import com.fisiogestao.solicitacoes.SolicitacaoAgendamentoEntity;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class AuditSnapshotFactory {

    public Map<String, Object> snapshot(Object source) {
        if (source == null) {
            return null;
        }
        if (source instanceof Map<?, ?> map) {
            LinkedHashMap<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, value) -> result.put(String.valueOf(key), value));
            return result;
        }
        if (source instanceof ClienteEntity cliente) {
            return snapshotCliente(cliente);
        }
        if (source instanceof SessaoEntity sessao) {
            return snapshotSessao(sessao);
        }
        if (source instanceof BloqueioAgendaEntity bloqueio) {
            return snapshotBloqueio(bloqueio);
        }
        if (source instanceof UsuarioSistemaEntity usuario) {
            return snapshotUsuario(usuario);
        }
        if (source instanceof InMemoryUserCatalog.SystemUserView usuarioView) {
            return snapshotUsuarioView(usuarioView);
        }
        if (source instanceof PlanoSessaoEntity plano) {
            return snapshotPlano(plano);
        }
        if (source instanceof LancamentoFinanceiroEntity lancamento) {
            return snapshotLancamento(lancamento);
        }
        if (source instanceof ProntuarioSoapEntity prontuario) {
            return snapshotProntuario(prontuario);
        }
        if (source instanceof ProntuarioAnexoEntity anexo) {
            return snapshotProntuarioAnexo(anexo);
        }
        if (source instanceof SolicitacaoAgendamentoEntity solicitacao) {
            return snapshotSolicitacao(solicitacao);
        }
        if (source instanceof EmailConfiguracaoEntity emailConfiguracao) {
            return snapshotEmailConfiguracao(emailConfiguracao);
        }
        return Map.of("valor", String.valueOf(source));
    }

    public String describe(Object source) {
        if (source == null) {
            return null;
        }
        if (source instanceof ClienteEntity cliente) {
            return identificar("Paciente", cliente.getId());
        }
        if (source instanceof SessaoEntity sessao) {
            return identificar("Sessao", sessao.getId()) + " - " + sessao.getDataHoraInicio();
        }
        if (source instanceof BloqueioAgendaEntity bloqueio) {
            return bloqueio.getMotivo();
        }
        if (source instanceof UsuarioSistemaEntity usuario) {
            return identificar("Usuario", usuario.getId());
        }
        if (source instanceof InMemoryUserCatalog.SystemUserView usuarioView) {
            return identificar("Usuario", usuarioView.id());
        }
        if (source instanceof PlanoSessaoEntity plano) {
            return plano.getNomePlano();
        }
        if (source instanceof LancamentoFinanceiroEntity lancamento) {
            return lancamento.getDescricao();
        }
        if (source instanceof ProntuarioSoapEntity prontuario) {
            return identificar("Registro SOAP", prontuario.getId());
        }
        if (source instanceof SolicitacaoAgendamentoEntity solicitacao) {
            return identificar("Solicitacao", solicitacao.getId());
        }
        if (source instanceof EmailConfiguracaoEntity emailConfiguracao) {
            return identificar("Configuracao de e-mail", emailConfiguracao.getId());
        }
        return String.valueOf(source);
    }

    private Map<String, Object> snapshotCliente(ClienteEntity cliente) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("id", cliente.getId());
        map.put("possuiTelefone", preenchido(cliente.getTelefone()));
        map.put("possuiEmail", preenchido(cliente.getEmail()));
        map.put("possuiObservacoes", preenchido(cliente.getObservacoes()));
        map.put("ativo", cliente.getAtivo());
        return map;
    }

    private Map<String, Object> snapshotSessao(SessaoEntity sessao) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("id", sessao.getId());
        map.put("clienteId", sessao.getCliente() != null ? sessao.getCliente().getId() : null);
        map.put("planoId", sessao.getPlanoSessao() != null ? sessao.getPlanoSessao().getId() : null);
        map.put("planoNome", sessao.getPlanoSessao() != null ? sessao.getPlanoSessao().getNomePlano() : null);
        map.put("dataHoraInicio", sessao.getDataHoraInicio());
        map.put("dataHoraFim", sessao.getDataHoraFim());
        map.put("status", sessao.getStatus());
        map.put("tipoAtendimento", sessao.getTipoAtendimento());
        map.put("possuiObservacoes", preenchido(sessao.getObservacoes()));
        map.put("planoConsumoRegistrado", sessao.getPlanoConsumoRegistrado());
        return map;
    }

    private Map<String, Object> snapshotBloqueio(BloqueioAgendaEntity bloqueio) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("id", bloqueio.getId());
        map.put("dataHoraInicio", bloqueio.getDataHoraInicio());
        map.put("dataHoraFim", bloqueio.getDataHoraFim());
        map.put("diaInteiro", bloqueio.getDiaInteiro());
        map.put("motivo", bloqueio.getMotivo());
        map.put("observacao", bloqueio.getObservacao());
        map.put("status", bloqueio.getStatus());
        map.put("origem", bloqueio.getOrigem());
        map.put("criadoPor", bloqueio.getCriadoPor());
        map.put("atualizadoPor", bloqueio.getAtualizadoPor());
        map.put("ativo", bloqueio.getAtivo());
        return map;
    }

    private Map<String, Object> snapshotUsuario(UsuarioSistemaEntity usuario) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("id", usuario.getId());
        map.put("possuiEmail", preenchido(usuario.getEmail()));
        map.put("role", usuario.getRole());
        return map;
    }

    private Map<String, Object> snapshotUsuarioView(InMemoryUserCatalog.SystemUserView usuario) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("id", usuario.id());
        map.put("possuiEmail", preenchido(usuario.email()));
        map.put("role", usuario.role());
        return map;
    }

    private Map<String, Object> snapshotPlano(PlanoSessaoEntity plano) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("id", plano.getId());
        map.put("clienteId", plano.getCliente() != null ? plano.getCliente().getId() : null);
        map.put("nomePlano", plano.getNomePlano());
        map.put("sessoesContratadas", plano.getSessoesContratadas());
        map.put("sessoesConsumidas", plano.getSessoesConsumidas());
        map.put("valorTotal", plano.getValorTotal());
        map.put("dataInicio", plano.getDataInicio());
        map.put("dataValidade", plano.getDataValidade());
        map.put("status", plano.getStatus());
        return map;
    }

    private Map<String, Object> snapshotLancamento(LancamentoFinanceiroEntity lancamento) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("id", lancamento.getId());
        map.put("tipo", lancamento.getTipo());
        map.put("status", lancamento.getStatus());
        map.put("descricao", lancamento.getDescricao());
        map.put("categoria", lancamento.getCategoria());
        map.put("valor", lancamento.getValor());
        map.put("dataRecebimento", lancamento.getDataRecebimento());
        map.put("dataPagamento", lancamento.getDataPagamento());
        map.put("clienteId", lancamento.getCliente() != null ? lancamento.getCliente().getId() : null);
        map.put("planoId", lancamento.getPlanoSessao() != null ? lancamento.getPlanoSessao().getId() : null);
        map.put("planoNome", lancamento.getPlanoSessao() != null ? lancamento.getPlanoSessao().getNomePlano() : null);
        return map;
    }

    private Map<String, Object> snapshotProntuario(ProntuarioSoapEntity prontuario) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("id", prontuario.getId());
        map.put("clienteId", prontuario.getCliente() != null ? prontuario.getCliente().getId() : null);
        map.put("sessaoId", prontuario.getSessao() != null ? prontuario.getSessao().getId() : null);
        map.put("tipoRegistro", prontuario.getTipoRegistro());
        map.put("conteudoClinicoHash", hashConteudoClinico(prontuario));
        map.put("possuiConteudoClinico", preenchido(prontuario.getTextoRegistro()) || prontuario.isEstruturada());
        map.put("possuiQueixaPrincipal", preenchido(prontuario.getQueixaPrincipal()));
        map.put("possuiAvaliacaoFisica", preenchido(prontuario.getAvaliacaoFisica()));
        map.put("possuiAvaliacaoFuncional", preenchido(prontuario.getAvaliacaoFuncional()));
        map.put("possuiObjetivosTratamento", preenchido(prontuario.getObjetivosTratamento()));
        map.put("possuiPlanoTerapeutico", preenchido(prontuario.getPlanoTerapeutico()));
        map.put("possuiObservacoesAvaliacao", preenchido(prontuario.getObservacoesAvaliacao()));
        map.put("criadoPor", prontuario.getCriadoPor());
        return map;
    }

    private Map<String, Object> snapshotProntuarioAnexo(ProntuarioAnexoEntity anexo) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("id", anexo.getId());
        map.put("clienteId", anexo.getCliente() != null ? anexo.getCliente().getId() : null);
        map.put("prontuarioSoapId", anexo.getProntuarioSoapId());
        map.put("nomeOriginalHash", hashTexto(anexo.getNomeOriginal()));
        map.put("tipoMime", anexo.getTipoMime());
        map.put("tamanhoBytes", anexo.getTamanhoBytes());
        map.put("possuiDescricao", preenchido(anexo.getDescricao()));
        map.put("criadoPor", anexo.getCriadoPor());
        map.put("tipoAnexo", anexo.getTipoAnexo());
        map.put("regiaoCorporal", anexo.getRegiaoCorporal());
        map.put("consentimentoConfirmadoEm", anexo.getConsentimentoConfirmadoEm());
        map.put("hashSha256", anexo.getHashSha256());
        // campo "dados" (BYTEA) intencionalmente omitido do snapshot de auditoria
        return map;
    }

    private Map<String, Object> snapshotSolicitacao(SolicitacaoAgendamentoEntity solicitacao) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("id", solicitacao.getId());
        map.put("possuiTelefone", preenchido(solicitacao.getTelefone()));
        map.put("possuiEmail", preenchido(solicitacao.getEmail()));
        map.put("dataHoraInicio", solicitacao.getDataHoraInicio());
        map.put("dataHoraFim", solicitacao.getDataHoraFim());
        map.put("status", solicitacao.getStatus());
        map.put("possuiObservacoes", preenchido(solicitacao.getObservacoes()));
        map.put("clienteId", solicitacao.getCliente() != null ? solicitacao.getCliente().getId() : null);
        return map;
    }

    private Map<String, Object> snapshotEmailConfiguracao(EmailConfiguracaoEntity configuracao) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("id", configuracao.getId());
        map.put("nomeRemetente", configuracao.getNomeRemetente());
        map.put("emailRemetenteConfigurado", preenchido(configuracao.getEmailRemetente()));
        map.put("smtpHostConfigurado", preenchido(configuracao.getSmtpHost()));
        map.put("smtpPort", configuracao.getSmtpPort());
        map.put("usaAutenticacao", configuracao.getUsaAutenticacao());
        map.put("usaTls", configuracao.getUsaTls());
        map.put("usaSsl", configuracao.getUsaSsl());
        map.put("replyToConfigurado", preenchido(configuracao.getReplyTo()));
        map.put("ativo", configuracao.getAtivo());
        map.put("possuiObservacoes", preenchido(configuracao.getObservacoes()));
        map.put("senhaConfigurada", configuracao.getSenhaAppCriptografada() != null && !configuracao.getSenhaAppCriptografada().isBlank());
        return map;
    }

    private boolean preenchido(String valor) {
        return valor != null && !valor.isBlank();
    }

    private String hashConteudoClinico(ProntuarioSoapEntity prontuario) {
        String conteudo = String.join("\u001f",
                valorSeguro(prontuario.getTextoRegistro()),
                valorSeguro(prontuario.getRegiaoCorporal()),
                valorSeguro(prontuario.getTecnicasUtilizadas()),
                valorSeguro(prontuario.getObsEvolucao()),
                valorSeguro(prontuario.getQueixaPrincipal()),
                valorSeguro(prontuario.getAvaliacaoFisica()),
                valorSeguro(prontuario.getAvaliacaoFuncional()),
                valorSeguro(prontuario.getObjetivosTratamento()),
                valorSeguro(prontuario.getPlanoTerapeutico()),
                valorSeguro(prontuario.getObservacoesAvaliacao()));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(conteudo.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("Nao foi possivel resumir o conteudo clinico para auditoria.", ex);
        }
    }

    private String valorSeguro(String valor) {
        return valor == null ? "" : valor;
    }

    private String identificar(String tipo, Object id) {
        return id == null ? tipo : tipo + " #" + id;
    }

    private String hashTexto(String valor) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(valorSeguro(valor).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("Nao foi possivel resumir o campo para auditoria.", ex);
        }
    }
}
