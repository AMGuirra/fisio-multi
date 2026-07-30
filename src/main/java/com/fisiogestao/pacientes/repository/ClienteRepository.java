package com.fisiogestao.pacientes.repository;

import com.fisiogestao.pacientes.ClienteEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClienteRepository extends JpaRepository<ClienteEntity, UUID> {

    /** Lista completa de ativos — mantida para contextos que precisam de todos os registros (ex.: seletores). */
    List<ClienteEntity> findByAtivoTrueOrderByNomeCompletoAsc();

    long countByAtivoTrue();

    long countByAtivoTrueAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            OffsetDateTime inicioInclusive,
            OffsetDateTime fimExclusivo);

    List<ClienteEntity> findTop6ByAtivoTrueOrderByCreatedAtDesc();

    Optional<ClienteEntity> findFirstByCpf(String cpf);

    Optional<ClienteEntity> findFirstByTelefone(String telefone);

    /**
     * Todos os pacientes com o celular informado. Usado no login por celular para detectar
     * ambiguidade (mais de um cadastro com o mesmo número) antes de autenticar.
     */
    List<ClienteEntity> findAllByTelefone(String telefone);

    Optional<ClienteEntity> findFirstByEmailIgnoreCase(String email);

    Optional<ClienteEntity> findByPortalToken(UUID portalToken);

    /**
     * Lista paginada de pacientes ativos com busca opcional por nome, CPF, telefone ou e-mail.
     *
     * <p>Quando {@code busca} é nulo ou vazio, retorna todos os ativos ordenados por nome.
     * Quando preenchido, filtra com ILIKE (case-insensitive) em quatro campos.</p>
     *
     * <p>Utilizar este método em vez de {@link #findByAtivoTrueOrderByNomeCompletoAsc()}
     * nas listagens de tela para evitar Full Table Scan seguido de ordenação em memória.</p>
     */
    @Query("""
            SELECT c FROM ClienteEntity c
            WHERE c.ativo = true
              AND (
                :busca IS NULL OR :busca = ''
                OR LOWER(c.nomeCompleto) LIKE LOWER(CONCAT('%', :busca, '%'))
                OR LOWER(COALESCE(c.cpf, ''))     LIKE LOWER(CONCAT('%', :busca, '%'))
                OR LOWER(COALESCE(c.telefone, '')) LIKE LOWER(CONCAT('%', :busca, '%'))
                OR LOWER(COALESCE(c.email, ''))    LIKE LOWER(CONCAT('%', :busca, '%'))
              )
            ORDER BY c.nomeCompleto ASC
            """)
    Page<ClienteEntity> findAtivosPaginado(@Param("busca") String busca, Pageable pageable);
}
