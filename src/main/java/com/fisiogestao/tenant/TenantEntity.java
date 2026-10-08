package com.fisiogestao.tenant;

import com.fisiogestao.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

/**
 * Base de toda entidade que pertence a uma clínica. O valor de {@code empresa_id} vem
 * sempre do {@link TenantContext} (via Hibernate) e não possui setter: não é possível
 * gravar um registro em outra empresa a partir de dados de formulário.
 */
@Getter
@MappedSuperclass
public abstract class TenantEntity extends BaseEntity {

    @TenantId
    @Column(name = "empresa_id", nullable = false, updatable = false)
    private UUID empresaId;
}
