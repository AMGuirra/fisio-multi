package com.fisiogestao.pacientes.repository;

import com.fisiogestao.pacientes.PlanoSessaoEntity;
import com.fisiogestao.pacientes.PlanoSessaoStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PlanoSessaoRepository extends JpaRepository<PlanoSessaoEntity, UUID> {

    @EntityGraph(attributePaths = "cliente")
    List<PlanoSessaoEntity> findByClienteIdOrderByCreatedAtDesc(UUID clienteId);

    boolean existsByClienteId(UUID clienteId);

    @Modifying
    @Query("UPDATE PlanoSessaoEntity p SET p.planoAnterior = null WHERE p.cliente.id = :clienteId")
    int limparPlanoAnteriorPorCliente(@Param("clienteId") UUID clienteId);

    void deleteByClienteId(UUID clienteId);

    @EntityGraph(attributePaths = "cliente")
    List<PlanoSessaoEntity> findByStatusOrderByCreatedAtDesc(PlanoSessaoStatus status);

    @EntityGraph(attributePaths = "cliente")
    List<PlanoSessaoEntity> findAllByOrderByCreatedAtDesc();

    List<PlanoSessaoEntity> findByPlanoAnteriorId(UUID planoAnteriorId);

}
