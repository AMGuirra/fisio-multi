package com.fisiogestao.landing;

import com.fisiogestao.config.ClinicTimezoneHolder;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "lp_config")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class LpConfigEntity {

    @Id
    @Column(length = 100)
    private String chave;

    @Column(columnDefinition = "TEXT")
    private String valor;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist @PreUpdate
    void touch() {
        updatedAt = ClinicTimezoneHolder.now();
    }
}
