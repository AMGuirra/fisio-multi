package com.fisiogestao.prontuario.repository;

import com.fisiogestao.prontuario.ProntuarioSoapEntity;
import com.fisiogestao.prontuario.TipoRegistroProntuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProntuarioSoapRepository extends JpaRepository<ProntuarioSoapEntity, UUID> {

    List<ProntuarioSoapEntity> findByClienteIdOrderByCreatedAtDesc(UUID clienteId);

    /**
     * LEFT JOIN FETCH de sessao: a view do prontuário (templates/prontuario/index.html)
     * lê registro.sessao.dataHoraInicio/feedbackEvaPaciente com open-in-view=false, ou
     * seja, sem sessão Hibernate ativa durante a renderização. Sem o fetch aqui, o
     * acesso ao proxy lazy de sessao lança LazyInitializationException a meio do
     * Thymeleaf, com resposta HTTP já parcialmente enviada.
     */
    @Query("select r from ProntuarioSoapEntity r left join fetch r.sessao "
            + "where r.cliente.id = :clienteId and r.tipoRegistro = :tipoRegistro "
            + "order by r.createdAt desc")
    List<ProntuarioSoapEntity> findByClienteIdAndTipoRegistroOrderByCreatedAtDesc(
            @Param("clienteId") UUID clienteId, @Param("tipoRegistro") TipoRegistroProntuario tipoRegistro);

    Optional<ProntuarioSoapEntity> findFirstByClienteIdAndTipoRegistro(
            UUID clienteId, TipoRegistroProntuario tipoRegistro);

    boolean existsByClienteId(UUID clienteId);

    void deleteByClienteId(UUID clienteId);
}
