package com.fisiogestao.auditoria.service;

import com.fisiogestao.agenda.BloqueioAgendaEntity;
import com.fisiogestao.agenda.SessaoEntity;
import com.fisiogestao.auth.InMemoryUserCatalog;
import com.fisiogestao.auth.UsuarioSistemaEntity;
import com.fisiogestao.email.EmailConfiguracaoEntity;
import com.fisiogestao.financeiro.LancamentoFinanceiroEntity;
import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.pacientes.PacoteSessaoEntity;
import com.fisiogestao.prontuario.ProntuarioSoapEntity;
import com.fisiogestao.solicitacoes.SolicitacaoAgendamentoEntity;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

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
        if (source instanceof PacoteSessaoEntity pacote) {
            return snapshotPacote(pacote);
        }
        if (source instanceof LancamentoFinanceiroEntity lancamento) {
            return snapshotLancamento(lancamento);
        }
        if (source instanceof ProntuarioSoapEntity prontuario) {
            return snapshotProntuario(prontuario);
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
            return cliente.getNomeCompleto();
        }
        if (source instanceof SessaoEntity sessao) {
            String nomePaciente = sessao.getCliente() != null ? sessao.getCliente().getNomeCompleto() : "Paciente não identificado";
            return nomePaciente + " - " + sessao.getDataHoraInicio();
        }
        if (source instanceof BloqueioAgendaEntity bloqueio) {
            return bloqueio.getMotivo();
        }
        if (source instanceof UsuarioSistemaEntity usuario) {
            return usuario.getNome();
        }
        if (source instanceof InMemoryUserCatalog.SystemUserView usuarioView) {
            return usuarioView.nome();
        }
        if (source instanceof PacoteSessaoEntity pacote) {
            return pacote.getNomePacote();
        }
        if (source instanceof LancamentoFinanceiroEntity lancamento) {
            return lancamento.getDescricao();
        }
        if (source instanceof ProntuarioSoapEntity prontuario) {
            return prontuario.getCliente() != null ? prontuario.getCliente().getNomeCompleto() : "Registro SOAP";
        }
        if (source instanceof SolicitacaoAgendamentoEntity solicitacao) {
            return solicitacao.getNomePaciente();
        }
        if (source instanceof EmailConfiguracaoEntity emailConfiguracao) {
            return emailConfiguracao.getNomeRemetente() + " - " + emailConfiguracao.getEmailRemetente();
        }
        return String.valueOf(source);
    }

    private Map<String, Object> snapshotCliente(ClienteEntity cliente) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("id", cliente.getId());
        map.put("nomeCompleto", cliente.getNomeCompleto());
        map.put("telefone", cliente.getTelefone());
        map.put("email", cliente.getEmail());
        map.put("dataNascimento", cliente.getDataNascimento());
        map.put("observacoes", cliente.getObservacoes());
        map.put("ativo", cliente.getAtivo());
        return map;
    }

    private Map<String, Object> snapshotSessao(SessaoEntity sessao) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("id", sessao.getId());
        map.put("clienteId", sessao.getCliente() != null ? sessao.getCliente().getId() : null);
        map.put("clienteNome", sessao.getCliente() != null ? sessao.getCliente().getNomeCompleto() : null);
        map.put("pacoteId", sessao.getPacoteSessao() != null ? sessao.getPacoteSessao().getId() : null);
        map.put("pacoteNome", sessao.getPacoteSessao() != null ? sessao.getPacoteSessao().getNomePacote() : null);
        map.put("dataHoraInicio", sessao.getDataHoraInicio());
        map.put("dataHoraFim", sessao.getDataHoraFim());
        map.put("status", sessao.getStatus());
        map.put("tipoAtendimento", sessao.getTipoAtendimento());
        map.put("observacoes", sessao.getObservacoes());
        map.put("pacoteConsumoRegistrado", sessao.getPacoteConsumoRegistrado());
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
        map.put("nome", usuario.getNome());
        map.put("username", usuario.getUsername());
        map.put("email", usuario.getEmail());
        map.put("role", usuario.getRole());
        return map;
    }

    private Map<String, Object> snapshotUsuarioView(InMemoryUserCatalog.SystemUserView usuario) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("id", usuario.id());
        map.put("nome", usuario.nome());
        map.put("username", usuario.username());
        map.put("email", usuario.email());
        map.put("role", usuario.role());
        map.put("label", usuario.label());
        return map;
    }

    private Map<String, Object> snapshotPacote(PacoteSessaoEntity pacote) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("id", pacote.getId());
        map.put("clienteId", pacote.getCliente() != null ? pacote.getCliente().getId() : null);
        map.put("clienteNome", pacote.getCliente() != null ? pacote.getCliente().getNomeCompleto() : null);
        map.put("nomePacote", pacote.getNomePacote());
        map.put("sessoesContratadas", pacote.getSessoesContratadas());
        map.put("sessoesConsumidas", pacote.getSessoesConsumidas());
        map.put("valorTotal", pacote.getValorTotal());
        map.put("dataInicio", pacote.getDataInicio());
        map.put("dataValidade", pacote.getDataValidade());
        map.put("status", pacote.getStatus());
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
        map.put("dataCompetencia", lancamento.getDataCompetencia());
        map.put("dataPagamento", lancamento.getDataPagamento());
        map.put("clienteId", lancamento.getCliente() != null ? lancamento.getCliente().getId() : null);
        map.put("clienteNome", lancamento.getCliente() != null ? lancamento.getCliente().getNomeCompleto() : null);
        map.put("pacoteId", lancamento.getPacoteSessao() != null ? lancamento.getPacoteSessao().getId() : null);
        map.put("pacoteNome", lancamento.getPacoteSessao() != null ? lancamento.getPacoteSessao().getNomePacote() : null);
        return map;
    }

    private Map<String, Object> snapshotProntuario(ProntuarioSoapEntity prontuario) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("id", prontuario.getId());
        map.put("clienteId", prontuario.getCliente() != null ? prontuario.getCliente().getId() : null);
        map.put("clienteNome", prontuario.getCliente() != null ? prontuario.getCliente().getNomeCompleto() : null);
        map.put("sessaoId", prontuario.getSessao() != null ? prontuario.getSessao().getId() : null);
        map.put("subjetivo", prontuario.getSubjetivo());
        map.put("objetivo", prontuario.getObjetivo());
        map.put("avaliacao", prontuario.getAvaliacao());
        map.put("plano", prontuario.getPlano());
        map.put("criadoPor", prontuario.getCriadoPor());
        return map;
    }

    private Map<String, Object> snapshotSolicitacao(SolicitacaoAgendamentoEntity solicitacao) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("id", solicitacao.getId());
        map.put("nomePaciente", solicitacao.getNomePaciente());
        map.put("telefone", solicitacao.getTelefone());
        map.put("email", solicitacao.getEmail());
        map.put("dataHoraInicio", solicitacao.getDataHoraInicio());
        map.put("dataHoraFim", solicitacao.getDataHoraFim());
        map.put("status", solicitacao.getStatus());
        map.put("observacoes", solicitacao.getObservacoes());
        map.put("clienteId", solicitacao.getCliente() != null ? solicitacao.getCliente().getId() : null);
        return map;
    }

    private Map<String, Object> snapshotEmailConfiguracao(EmailConfiguracaoEntity configuracao) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("id", configuracao.getId());
        map.put("nomeRemetente", configuracao.getNomeRemetente());
        map.put("emailRemetente", configuracao.getEmailRemetente());
        map.put("smtpHost", configuracao.getSmtpHost());
        map.put("smtpPort", configuracao.getSmtpPort());
        map.put("usaAutenticacao", configuracao.getUsaAutenticacao());
        map.put("usaTls", configuracao.getUsaTls());
        map.put("usaSsl", configuracao.getUsaSsl());
        map.put("replyTo", configuracao.getReplyTo());
        map.put("ativo", configuracao.getAtivo());
        map.put("observacoes", configuracao.getObservacoes());
        map.put("senhaConfigurada", configuracao.getSenhaAppCriptografada() != null && !configuracao.getSenhaAppCriptografada().isBlank());
        return map;
    }
}
