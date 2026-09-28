package com.accentra.leavemanagement.repository;

import com.accentra.leavemanagement.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    @Query("select e from Employee e join fetch e.user left join fetch e.team left join fetch e.manager where e.user.id = :userId")
    Optional<Employee> findByUserId(@Param("userId") Long userId);

    @Query("select e from Employee e join fetch e.user where e.team.id = :teamId order by e.fullName")
    List<Employee> findTeamMembers(@Param("teamId") Long teamId);

    long countByTeamId(Long teamId);

    List<Employee> findByManagerIdOrderByFullName(Long managerId);
}
