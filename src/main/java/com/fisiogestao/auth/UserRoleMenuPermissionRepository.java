package com.fisiogestao.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserRoleMenuPermissionRepository extends JpaRepository<UserRoleMenuPermissionEntity, UUID> {

    List<UserRoleMenuPermissionEntity> findByRoleOrderByMenuKeyAsc(UserRole role);

    boolean existsByRole(UserRole role);

    void deleteByRole(UserRole role);
}
