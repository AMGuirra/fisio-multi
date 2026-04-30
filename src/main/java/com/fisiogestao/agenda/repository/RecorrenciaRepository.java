package com.fisiogestao.agenda.repository;

import com.fisiogestao.agenda.RecorrenciaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecorrenciaRepository extends JpaRepository<RecorrenciaEntity, Long> {
}
