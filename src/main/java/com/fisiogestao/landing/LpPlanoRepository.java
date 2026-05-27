package com.fisiogestao.landing;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface LpPlanoRepository extends JpaRepository<LpPlanoEntity, UUID> {
    List<LpPlanoEntity> findAllByAtivoTrueOrderByOrdemAsc();
    List<LpPlanoEntity> findAllByOrderByOrdemAsc();
}
