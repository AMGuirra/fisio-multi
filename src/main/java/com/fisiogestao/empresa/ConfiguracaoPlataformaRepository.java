package com.fisiogestao.empresa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConfiguracaoPlataformaRepository extends JpaRepository<ConfiguracaoPlataformaEntity, UUID> {
}
