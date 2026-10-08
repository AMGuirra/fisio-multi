package com.fisiogestao.email.repository;

import com.fisiogestao.email.EmailConfiguracaoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** SMTP por clínica ({@code empresa_id}) e SMTP global da plataforma ({@code empresa_id IS NULL}). */
public interface EmailConfiguracaoRepository extends JpaRepository<EmailConfiguracaoEntity, UUID> {

    Optional<EmailConfiguracaoEntity> findFirstByEmpresaIdAndAtivoTrueOrderByUpdatedAtDesc(UUID empresaId);

    Optional<EmailConfiguracaoEntity> findFirstByEmpresaIdIsNullAndAtivoTrueOrderByUpdatedAtDesc();

    List<EmailConfiguracaoEntity> findAllByEmpresaIdOrderByAtivoDescUpdatedAtDesc(UUID empresaId);

    List<EmailConfiguracaoEntity> findAllByEmpresaIdIsNullOrderByAtivoDescUpdatedAtDesc();

    Optional<EmailConfiguracaoEntity> findByIdAndEmpresaId(UUID id, UUID empresaId);

    Optional<EmailConfiguracaoEntity> findByIdAndEmpresaIdIsNull(UUID id);
}
