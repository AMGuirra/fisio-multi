package com.fisiogestao.whatsapp.repository;

import com.fisiogestao.whatsapp.MensagemWhatsAppEntity;
import com.fisiogestao.whatsapp.StatusMensagemWhatsApp;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface MensagemWhatsAppRepository extends JpaRepository<MensagemWhatsAppEntity, UUID> {

    List<MensagemWhatsAppEntity> findAllByOrderByCreatedAtDesc();

    List<MensagemWhatsAppEntity> findAllByStatusOrderByCreatedAtDesc(StatusMensagemWhatsApp status);

    long countByStatus(StatusMensagemWhatsApp status);

    /**
     * Retorna mensagens PENDENTE ordenadas por data de criação (mais antigas primeiro),
     * respeitando o limite de página para evitar sobrecarga em lotes grandes.
     */
    @Query("SELECT m FROM MensagemWhatsAppEntity m WHERE m.status = :status ORDER BY m.createdAt ASC")
    List<MensagemWhatsAppEntity> findPendentesParaEnvio(
            @org.springframework.data.repository.query.Param("status") StatusMensagemWhatsApp status,
            Pageable pageable);

}
