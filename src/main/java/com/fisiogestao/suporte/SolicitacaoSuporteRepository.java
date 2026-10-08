package com.fisiogestao.suporte;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface SolicitacaoSuporteRepository extends JpaRepository<SolicitacaoSuporteEntity, UUID> {

    long countByStatus(StatusSolicitacaoSuporte status);

    @Query("""
            select s from SolicitacaoSuporteEntity s
            where (:status is null or s.status = :status)
              and (:termo = '%'
                   or lower(s.nome) like :termo
                   or lower(s.email) like :termo
                   or lower(s.descricao) like :termo
                   or coalesce(s.cpf, '') like :digitos
                   or coalesce(s.cnpj, '') like :digitos)
            """)
    Page<SolicitacaoSuporteEntity> buscar(
            @Param("status") StatusSolicitacaoSuporte status,
            @Param("termo") String termo,
            @Param("digitos") String digitos,
            Pageable pageable
    );
}
