package com.fisiogestao.solicitacoes.service;

import com.fisiogestao.listaespera.ListaEsperaService;
import com.fisiogestao.solicitacoes.SolicitacaoAgendamentoEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Coordena a rejeicao com a inclusao opcional na lista de espera.
 *
 * <p>As duas alteracoes precisam compartilhar a mesma transacao: alem de manter
 * o cliente associado carregado durante a inclusao na fila, isso impede que a
 * solicitacao fique rejeitada caso a criacao da entrada na fila falhe.</p>
 */
@Service
@RequiredArgsConstructor
public class RejeicaoSolicitacaoService {

    private final SolicitacaoAgendamentoService solicitacaoAgendamentoService;
    private final ListaEsperaService listaEsperaService;

    @Transactional
    public SolicitacaoAgendamentoEntity rejeitar(UUID solicitacaoId, boolean adicionarListaEspera) {
        SolicitacaoAgendamentoEntity solicitacao = adicionarListaEspera
                ? solicitacaoAgendamentoService.colocarEmEspera(solicitacaoId)
                : solicitacaoAgendamentoService.rejeitarSolicitacao(solicitacaoId);

        if (adicionarListaEspera && solicitacao.getCliente() != null) {
            listaEsperaService.adicionarDaRejeicao(solicitacao.getCliente());
        }

        return solicitacao;
    }
}
