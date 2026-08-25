package com.fisiogestao.email.repository;

import com.fisiogestao.email.EmailLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EmailLogRepository extends JpaRepository<EmailLogEntity, UUID> {

    void deleteByEntidadeTipoAndEntidadeId(String entidadeTipo, UUID entidadeId);

    List<EmailLogEntity> findTop50ByOrderByCreatedAtDesc();
}
