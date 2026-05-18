package com.fisiogestao.listaespera;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ListaEsperaRepository extends JpaRepository<ListaEsperaEntity, UUID> {

    @EntityGraph(attributePaths = {"cliente"})
    List<ListaEsperaEntity> findByStatusOrderByCreatedAtAsc(StatusListaEspera status);

    @EntityGraph(attributePaths = {"cliente"})
    List<ListaEsperaEntity> findByClienteIdOrderByCreatedAtDesc(UUID clienteId);

    boolean existsByClienteIdAndStatus(UUID clienteId, StatusListaEspera status);

    long countByStatus(StatusListaEspera status);

    Optional<ListaEsperaEntity> findByIdAndClienteId(UUID id, UUID clienteId);
}
