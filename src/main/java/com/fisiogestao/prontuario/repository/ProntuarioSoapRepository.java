package com.fisiogestao.prontuario.repository;

import com.fisiogestao.prontuario.ProntuarioSoapEntity;
import com.fisiogestao.prontuario.TipoRegistroProntuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProntuarioSoapRepository extends JpaRepository<ProntuarioSoapEntity, UUID> {

    List<ProntuarioSoapEntity> findByClienteIdOrderByCreatedAtDesc(UUID clienteId);

    List<ProntuarioSoapEntity> findByClienteIdAndTipoRegistroOrderByCreatedAtDesc(
            UUID clienteId, TipoRegistroProntuario tipoRegistro);

    Optional<ProntuarioSoapEntity> findFirstByClienteIdAndTipoRegistro(
            UUID clienteId, TipoRegistroProntuario tipoRegistro);

    boolean existsByClienteId(UUID clienteId);

    void deleteByClienteId(UUID clienteId);
}
