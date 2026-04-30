package com.fisiogestao.pacientes.repository;

import com.fisiogestao.pacientes.ClienteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClienteRepository extends JpaRepository<ClienteEntity, UUID> {

    List<ClienteEntity> findByAtivoTrueOrderByNomeCompletoAsc();

    Optional<ClienteEntity> findFirstByCpf(String cpf);

    Optional<ClienteEntity> findFirstByTelefone(String telefone);

    Optional<ClienteEntity> findFirstByEmailIgnoreCase(String email);
}
