package com.accentra.leavemanagement.repository;

import com.accentra.leavemanagement.entity.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeaveTypeRepository extends JpaRepository<LeaveType, Long> {

    Optional<LeaveType> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    List<LeaveType> findAllByActiveTrueOrderByName();
}
