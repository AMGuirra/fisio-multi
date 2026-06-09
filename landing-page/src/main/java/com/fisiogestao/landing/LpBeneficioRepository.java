package com.fisiogestao.landing;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface LpBeneficioRepository extends JpaRepository<LpBeneficioEntity, UUID> {
    List<LpBeneficioEntity> findAllByAtivoTrueOrderByOrdemAsc();
    List<LpBeneficioEntity> findAllByOrderByOrdemAsc();
}
