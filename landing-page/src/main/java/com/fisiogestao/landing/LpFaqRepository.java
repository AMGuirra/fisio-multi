package com.fisiogestao.landing;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface LpFaqRepository extends JpaRepository<LpFaqEntity, UUID> {
    List<LpFaqEntity> findAllByAtivoTrueOrderByOrdemAsc();
    List<LpFaqEntity> findAllByOrderByOrdemAsc();
}
