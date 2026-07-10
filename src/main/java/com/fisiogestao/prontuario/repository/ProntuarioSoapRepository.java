package com.fisiogestao.prontuario.repository;

import com.fisiogestao.prontuario.ProntuarioSoapEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProntuarioSoapRepository extends JpaRepository<ProntuarioSoapEntity, UUID> {

    List<ProntuarioSoapEntity> findByClienteIdOrderByCreatedAtDesc(UUID clienteId);

    boolean existsByClienteId(UUID clienteId);

    void deleteByClienteId(UUID clienteId);
}
