package com.fisiogestao.email.repository;

import com.fisiogestao.email.EmailConfiguracaoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmailConfiguracaoRepository extends JpaRepository<EmailConfiguracaoEntity, UUID> {

    Optional<EmailConfiguracaoEntity> findFirstByAtivoTrueOrderByUpdatedAtDesc();

    List<EmailConfiguracaoEntity> findAllByOrderByAtivoDescUpdatedAtDesc();
}
