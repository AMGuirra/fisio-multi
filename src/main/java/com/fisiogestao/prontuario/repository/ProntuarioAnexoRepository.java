package com.fisiogestao.prontuario.repository;

import com.fisiogestao.prontuario.ProntuarioAnexoEntity;
import com.fisiogestao.prontuario.dto.ProntuarioAnexoResumoDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProntuarioAnexoRepository extends JpaRepository<ProntuarioAnexoEntity, UUID> {

    /**
     * Retorna os metadados dos anexos de um paciente sem carregar o campo {@code dados} (BYTEA).
     * Ordenado do mais recente ao mais antigo.
     */
    @Query("""
            SELECT new com.fisiogestao.prontuario.dto.ProntuarioAnexoResumoDto(
                a.id,
                a.nomeOriginal,
                a.tipoMime,
                a.tamanhoBytes,
                a.descricao,
                a.criadoPor,
                a.createdAt,
                a.prontuarioSoapId
            )
            FROM ProntuarioAnexoEntity a
            WHERE a.cliente.id = :clienteId
            ORDER BY a.createdAt DESC
            """)
    List<ProntuarioAnexoResumoDto> findResumosByClienteId(@Param("clienteId") UUID clienteId);
}
