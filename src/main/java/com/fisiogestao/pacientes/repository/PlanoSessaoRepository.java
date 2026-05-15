package com.fisiogestao.pacientes.repository;

import com.fisiogestao.pacientes.PlanoSessaoEntity;
import com.fisiogestao.pacientes.PlanoSessaoStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlanoSessaoRepository extends JpaRepository<PlanoSessaoEntity, UUID> {

    @EntityGraph(attributePaths = "cliente")
    List<PlanoSessaoEntity> findByClienteIdOrderByCreatedAtDesc(UUID clienteId);

    boolean existsByClienteId(UUID clienteId);

    @EntityGraph(attributePaths = "cliente")
    List<PlanoSessaoEntity> findByStatusOrderByCreatedAtDesc(PlanoSessaoStatus status);

}
