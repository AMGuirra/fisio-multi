package com.fisiogestao.auditoria;

import com.fisiogestao.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "auditoria_sistema_campo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class AuditFieldChangeEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auditoria_id", nullable = false)
    private AuditEventEntity auditEvent;

    @Column(name = "campo_alterado", nullable = false, length = 120)
    private String fieldName;

    @Column(name = "valor_anterior", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "valor_novo", columnDefinition = "TEXT")
    private String newValue;
}
