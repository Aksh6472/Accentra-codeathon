package com.accentra.leavemanagement.repository;

import com.accentra.leavemanagement.entity.User;
import com.accentra.leavemanagement.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    List<User> findAllByRoleAndEnabledTrue(Role role);
}
