package com.fisiogestao.gestor;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificacaoGestorRepository extends JpaRepository<NotificacaoGestorEntity, UUID> {

    List<NotificacaoGestorEntity> findTop20ByOrderByCreatedAtDesc();

    List<NotificacaoGestorEntity> findTop10ByLidaFalseOrderByCreatedAtDesc();

    long countByLidaFalse();

    List<NotificacaoGestorEntity> findByTipoAndEmpresaId(TipoNotificacaoGestor tipo, UUID empresaId);
}
