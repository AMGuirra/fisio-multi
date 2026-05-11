package com.fisiogestao.whatsapp.service;

import com.fisiogestao.whatsapp.MensagemWhatsAppEntity;
import com.fisiogestao.whatsapp.StatusMensagemWhatsApp;
import com.fisiogestao.whatsapp.repository.MensagemWhatsAppRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WhatsAppService {

    private final MensagemWhatsAppRepository mensagemWhatsAppRepository;

    public List<MensagemWhatsAppEntity> listarFila() {
        return mensagemWhatsAppRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<MensagemWhatsAppEntity> listarFila(StatusMensagemWhatsApp status) {
        if (status == null) {
            return listarFila();
        }
        return mensagemWhatsAppRepository.findAllByStatusOrderByCreatedAtDesc(status);
    }

    public long totalPendentes() {
        return mensagemWhatsAppRepository.countByStatus(StatusMensagemWhatsApp.PENDENTE);
    }

    public long totalPorStatus(StatusMensagemWhatsApp status) {
        if (status == null) {
            return mensagemWhatsAppRepository.count();
        }
        return mensagemWhatsAppRepository.countByStatus(status);
    }

    @Transactional
    public MensagemWhatsAppEntity enfileirar(String destinatario, String conteudo, String referencia) {
        if (destinatario == null || destinatario.isBlank() || conteudo == null || conteudo.isBlank()) {
            return null;
        }

        MensagemWhatsAppEntity mensagem = MensagemWhatsAppEntity.builder()
                .destinatario(destinatario.replaceAll("\\D", ""))
                .conteudo(conteudo.trim())
                .referencia(referencia)
                .agendadaPara(OffsetDateTime.now())
                .status(StatusMensagemWhatsApp.PENDENTE)
                .tentativas(0)
                .build();
        return mensagemWhatsAppRepository.save(mensagem);
    }

    /**
     * Recoloca uma mensagem com status ERRO de volta na fila (PENDENTE),
     * zerando tentativas e erro para que o processador tente novamente.
     */
    @Transactional
    public void recolocarNaFila(UUID id) {
        mensagemWhatsAppRepository.findById(id).ifPresent(msg -> {
            msg.setStatus(StatusMensagemWhatsApp.PENDENTE);
            msg.setTentativas(0);
            msg.setErro(null);
            msg.setAgendadaPara(OffsetDateTime.now());
            mensagemWhatsAppRepository.save(msg);
        });
    }
}
