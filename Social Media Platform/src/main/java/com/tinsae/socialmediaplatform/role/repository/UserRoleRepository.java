package com.tinsae.socialmediaplatform.role.repository;

import com.tinsae.socialmediaplatform.role.entity.UserRole;
import com.tinsae.socialmediaplatform.role.entity.UserRoleId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    @EntityGraph(attributePaths = "role")
    List<UserRole> findByIdUserId(UUID userId);
}
