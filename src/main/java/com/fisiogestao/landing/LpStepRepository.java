package com.fisiogestao.landing;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface LpStepRepository extends JpaRepository<LpStepEntity, UUID> {
    List<LpStepEntity> findAllByAtivoTrueOrderByOrdemAsc();
    List<LpStepEntity> findAllByOrderByOrdemAsc();
}
