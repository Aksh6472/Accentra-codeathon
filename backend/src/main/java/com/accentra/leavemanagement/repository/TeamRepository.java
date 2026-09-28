package com.accentra.leavemanagement.repository;

import com.accentra.leavemanagement.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {

    List<Team> findByManagerIdOrderByName(Long managerId);

    List<Team> findAllByOrderByName();

    Optional<Team> findByName(String name);
}
