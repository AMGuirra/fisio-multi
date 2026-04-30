package com.fisiogestao.status;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface SystemStatusRepository extends JpaRepository<SystemStatusEntity, UUID> {

    Optional<SystemStatusEntity> findFirstByOrderByAtualizadoEmDesc();
}
