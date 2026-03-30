package com.fisiogestao.agenda.repository;

import com.fisiogestao.agenda.ConfiguracaoClinicaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ConfiguracaoClinicaRepository extends JpaRepository<ConfiguracaoClinicaEntity, UUID> {

    Optional<ConfiguracaoClinicaEntity> findFirstByAtivoTrueOrderByCreatedAtAsc();
}
