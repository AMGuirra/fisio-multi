package com.fisiogestao.whatsapp.repository;

import com.fisiogestao.whatsapp.MensagemWhatsAppEntity;
import com.fisiogestao.whatsapp.StatusMensagemWhatsApp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MensagemWhatsAppRepository extends JpaRepository<MensagemWhatsAppEntity, UUID> {

    List<MensagemWhatsAppEntity> findAllByOrderByCreatedAtDesc();

    List<MensagemWhatsAppEntity> findAllByStatusOrderByCreatedAtDesc(StatusMensagemWhatsApp status);

    long countByStatus(StatusMensagemWhatsApp status);
}
