package com.fisiogestao.empresa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmpresaRepository extends JpaRepository<EmpresaEntity, UUID> {

    Optional<EmpresaEntity> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    boolean existsByCnpj(String cnpj);

    boolean existsByCnpjAndIdNot(String cnpj, UUID id);

    Optional<EmpresaEntity> findFirstByCnpj(String cnpj);

    List<EmpresaEntity> findByCpf(String cpf);

    List<EmpresaEntity> findByStatus(EmpresaStatus status);

    long countByStatus(EmpresaStatus status);

    long countByCreatedAtGreaterThanEqual(OffsetDateTime inicio);

    long countByStatusAndDataFimTesteBefore(EmpresaStatus status, OffsetDateTime referencia);

    long countByStatusAndDataFimTesteBetween(EmpresaStatus status, OffsetDateTime inicio, OffsetDateTime fim);

    List<EmpresaEntity> findByStatusAndDataFimTesteBetweenOrderByDataFimTesteAsc(
            EmpresaStatus status, OffsetDateTime inicio, OffsetDateTime fim);

    List<EmpresaEntity> findTop8ByOrderByCreatedAtDesc();

    @Query("""
            select e from EmpresaEntity e
            where (:status is null or e.status = :status)
              and (:termo = '%'
                   or lower(e.nome) like :termo
                   or lower(e.slug) like :termo
                   or lower(coalesce(e.email, '')) like :termo
                   or coalesce(e.cpf, '') like :digitos
                   or coalesce(e.cnpj, '') like :digitos)
            """)
    Page<EmpresaEntity> buscar(
            @Param("status") EmpresaStatus status,
            @Param("termo") String termo,
            @Param("digitos") String digitos,
            Pageable pageable
    );
}
