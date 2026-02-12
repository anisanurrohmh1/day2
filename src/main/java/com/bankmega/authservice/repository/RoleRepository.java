package com.bankmega.authservice.repository;

import com.bankmega.authservice.entity.Role;
import com.bankmega.authservice.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {

    Optional<Role> findByRoleName(RoleName roleName);
}
