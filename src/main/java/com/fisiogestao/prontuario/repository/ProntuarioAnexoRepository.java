package com.fisiogestao.prontuario.repository;

import com.fisiogestao.prontuario.ProntuarioAnexoEntity;
import com.fisiogestao.prontuario.TipoAnexoProntuario;
import com.fisiogestao.prontuario.dto.ProntuarioAnexoResumoDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProntuarioAnexoRepository extends JpaRepository<ProntuarioAnexoEntity, UUID> {

    String SELECT_RESUMO = """
            SELECT new com.fisiogestao.prontuario.dto.ProntuarioAnexoResumoDto(
                a.id,
                a.nomeOriginal,
                a.tipoMime,
                a.tamanhoBytes,
                a.descricao,
                a.criadoPor,
                a.createdAt,
                a.prontuarioSoapId,
                a.tipoAnexo,
                a.regiaoCorporal
            )
            FROM ProntuarioAnexoEntity a
            """;

    /**
     * Retorna os metadados dos anexos de um paciente sem carregar o campo {@code dados} (BYTEA).
     * Ordenado do mais recente ao mais antigo.
     */
    @Query(SELECT_RESUMO + " WHERE a.cliente.id = :clienteId ORDER BY a.createdAt DESC")
    List<ProntuarioAnexoResumoDto> findResumosByClienteId(@Param("clienteId") UUID clienteId);

    /** Mesmo resumo, filtrado por tipo de anexo (documento genérico ou foto clínica). */
    @Query(SELECT_RESUMO + " WHERE a.cliente.id = :clienteId AND a.tipoAnexo = :tipoAnexo ORDER BY a.createdAt DESC")
    List<ProntuarioAnexoResumoDto> findResumosByClienteIdAndTipoAnexo(
            @Param("clienteId") UUID clienteId, @Param("tipoAnexo") TipoAnexoProntuario tipoAnexo);

    /** Fotos clínicas vinculadas a uma evolução específica, para exibição no histórico. */
    @Query(SELECT_RESUMO + " WHERE a.prontuarioSoapId = :prontuarioSoapId AND a.tipoAnexo = 'FOTO_CLINICA' ORDER BY a.createdAt ASC")
    List<ProntuarioAnexoResumoDto> findFotosByProntuarioSoapId(@Param("prontuarioSoapId") UUID prontuarioSoapId);

    void deleteByClienteId(UUID clienteId);
}
