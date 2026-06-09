package com.fisiogestao.landing;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface LpTestemunhoRepository extends JpaRepository<LpTestemunhoEntity, UUID> {
    List<LpTestemunhoEntity> findAllByAtivoTrueOrderByOrdemAsc();
    List<LpTestemunhoEntity> findAllByOrderByOrdemAsc();
}
