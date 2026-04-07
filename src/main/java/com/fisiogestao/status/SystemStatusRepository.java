package com.fisiogestao.status;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
import java.util.UUID;

public interface SystemStatusRepository extends JpaRepository<SystemStatusEntity, UUID> {

    @Query("SELECT s FROM SystemStatusEntity s ORDER BY s.atualizadoEm DESC LIMIT 1")
    Optional<SystemStatusEntity> findUltimoRegistro();
}