package com.fisiogestao.listaespera;

import com.fisiogestao.auditoria.AuditActionType;
import com.fisiogestao.auditoria.AuditEventOrigin;
import com.fisiogestao.auditoria.AuditModule;
import com.fisiogestao.auditoria.service.AuditService;
import com.fisiogestao.config.ClinicTimezoneHolder;
import com.fisiogestao.pacientes.ClienteEntity;
import com.fisiogestao.shared.web.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListaEsperaService {

    private final ListaEsperaRepository listaEsperaRepository;
    private final AuditService auditService;
    private final ListaEsperaEmailService emailService;

    @Transactional(readOnly = true)
    public List<ListaEsperaEntity> listarAguardando() {
        return listaEsperaRepository.findByStatusOrderByCreatedAtAsc(StatusListaEspera.AGUARDANDO);
    }

    @Transactional(readOnly = true)
    public List<ListaEsperaEntity> listarPorCliente(UUID clienteId) {
        return listaEsperaRepository.findByClienteIdOrderByCreatedAtDesc(clienteId);
    }

    @Transactional
    public ListaEsperaEntity entrarNaFila(ClienteEntity cliente,
                                          PeriodoPreferido periodo,
                                          String diasSemana,
                                          String observacoes) {
        if (listaEsperaRepository.existsByClienteIdAndStatus(cliente.getId(), StatusListaEspera.AGUARDANDO)) {
            throw new BusinessException("Você já está na lista de espera. Aguarde o contato da clínica.");
        }

        ListaEsperaEntity entrada = ListaEsperaEntity.builder()
                .cliente(cliente)
                .periodoPreferido(periodo != null ? periodo : PeriodoPreferido.QUALQUER)
                .diasSemana(diasSemana)
                .observacoes(observacoes)
                .status(StatusListaEspera.AGUARDANDO)
                .build();

        ListaEsperaEntity salvo = listaEsperaRepository.save(entrada);
        auditService.recordCreate(
                AuditModule.LISTA_ESPERA,
                "lista_espera",
                salvo,
                null,
                "Paciente entrou na lista de espera via portal.",
                AuditEventOrigin.ONLINE
        );
        emailService.enviarConfirmacaoEntrada(salvo);
        return salvo;
    }

    /** Entrada anônima via formulário público (sem ClienteEntity vinculado). */
    @Transactional
    public ListaEsperaEntity entrarNaFilaPublico(String nome, String telefone, String email,
                                                  PeriodoPreferido periodo, String observacoes) {
        ListaEsperaEntity entrada = ListaEsperaEntity.builder()
                .nomePublico(nome != null ? nome.trim() : null)
                .telefonePublico(telefone != null ? telefone.replaceAll("\\D", "") : null)
                .emailPublico(email != null && !email.isBlank() ? email.trim() : null)
                .periodoPreferido(periodo != null ? periodo : PeriodoPreferido.QUALQUER)
                .observacoes(observacoes != null && !observacoes.isBlank() ? observacoes.trim() : null)
                .status(StatusListaEspera.AGUARDANDO)
                .build();
        ListaEsperaEntity salvo = listaEsperaRepository.save(entrada);
        emailService.enviarConfirmacaoEntrada(salvo);
        return salvo;
    }

    /** Entrada criada internamente quando uma solicitação é rejeitada (paciente já cadastrado). */
    @Transactional
    public ListaEsperaEntity adicionarDaRejeicao(ClienteEntity cliente) {
        if (listaEsperaRepository.existsByClienteIdAndStatus(cliente.getId(), StatusListaEspera.AGUARDANDO)) {
            return null;
        }
        ListaEsperaEntity entrada = ListaEsperaEntity.builder()
                .cliente(cliente)
                .periodoPreferido(PeriodoPreferido.QUALQUER)
                .observacoes("Adicionado automaticamente após rejeição de solicitação de agendamento.")
                .status(StatusListaEspera.AGUARDANDO)
                .build();
        ListaEsperaEntity salvo = listaEsperaRepository.save(entrada);
        emailService.enviarConfirmacaoEntrada(salvo);
        return salvo;
    }

    @Transactional(readOnly = true)
    public long contarAguardando() {
        return listaEsperaRepository.countByStatus(StatusListaEspera.AGUARDANDO);
    }

    @Transactional
    public void notificar(UUID id) {
        ListaEsperaEntity entrada = buscarPorId(id);
        if (entrada.getStatus() != StatusListaEspera.AGUARDANDO) {
            throw new BusinessException("Apenas entradas em espera podem ser notificadas.");
        }
        var snapshot = auditService.snapshotOf(entrada);
        entrada.setStatus(StatusListaEspera.NOTIFICADO);
        entrada.setNotificadoEm(ClinicTimezoneHolder.now());
        ListaEsperaEntity salvo = listaEsperaRepository.save(entrada);
        auditService.recordManual(
                AuditModule.LISTA_ESPERA,
                "lista_espera",
                salvo.getId().toString(),
                salvo.getNomeExibicao(),
                AuditActionType.UPDATE,
                snapshot,
                auditService.snapshotOf(salvo),
                null,
                "Paciente notificado sobre vaga disponível.",
                AuditEventOrigin.MANUAL
        );
        emailService.enviarNotificacaoVaga(salvo);
    }

    @Transactional
    public ListaEsperaEntity converter(UUID id) {
        ListaEsperaEntity entrada = buscarPorId(id);
        if (entrada.getStatus() == StatusListaEspera.CANCELADO) {
            throw new BusinessException("Entrada cancelada não pode ser convertida.");
        }
        var snapshot = auditService.snapshotOf(entrada);
        entrada.setStatus(StatusListaEspera.CONVERTIDO);
        ListaEsperaEntity salvo = listaEsperaRepository.save(entrada);
        auditService.recordManual(
                AuditModule.LISTA_ESPERA,
                "lista_espera",
                salvo.getId().toString(),
                salvo.getNomeExibicao(),
                AuditActionType.UPDATE,
                snapshot,
                auditService.snapshotOf(salvo),
                null,
                "Entrada convertida em agendamento.",
                AuditEventOrigin.MANUAL
        );
        return salvo;
    }

    @Transactional
    public void cancelar(UUID id) {
        ListaEsperaEntity entrada = buscarPorId(id);
        if (entrada.getStatus() == StatusListaEspera.CANCELADO) {
            return;
        }
        var snapshot = auditService.snapshotOf(entrada);
        entrada.setStatus(StatusListaEspera.CANCELADO);
        ListaEsperaEntity salvo = listaEsperaRepository.save(entrada);
        auditService.recordManual(
                AuditModule.LISTA_ESPERA,
                "lista_espera",
                salvo.getId().toString(),
                salvo.getNomeExibicao(),
                AuditActionType.CANCEL,
                snapshot,
                auditService.snapshotOf(salvo),
                null,
                "Entrada na lista de espera cancelada.",
                AuditEventOrigin.MANUAL
        );
    }

    @Transactional
    public void cancelarPeloPaciente(UUID id, UUID clienteId) {
        ListaEsperaEntity entrada = listaEsperaRepository.findByIdAndClienteId(id, clienteId)
                .orElseThrow(() -> new BusinessException("Entrada não encontrada."));
        if (entrada.getStatus() != StatusListaEspera.AGUARDANDO) {
            throw new BusinessException("Não é possível cancelar esta entrada.");
        }
        entrada.setStatus(StatusListaEspera.CANCELADO);
        listaEsperaRepository.save(entrada);
    }

    private ListaEsperaEntity buscarPorId(UUID id) {
        return listaEsperaRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Entrada na lista de espera não encontrada."));
    }
}
