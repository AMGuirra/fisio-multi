package com.fisiogestao.pacientes.repository;

import com.fisiogestao.pacientes.PacoteSessaoEntity;
import com.fisiogestao.pacientes.PacoteSessaoStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PacoteSessaoRepository extends JpaRepository<PacoteSessaoEntity, UUID> {

    @EntityGraph(attributePaths = "cliente")
    List<PacoteSessaoEntity> findByClienteIdOrderByCreatedAtDesc(UUID clienteId);

    boolean existsByClienteId(UUID clienteId);

    @EntityGraph(attributePaths = "cliente")
    List<PacoteSessaoEntity> findByStatusOrderByCreatedAtDesc(PacoteSessaoStatus status);

}
