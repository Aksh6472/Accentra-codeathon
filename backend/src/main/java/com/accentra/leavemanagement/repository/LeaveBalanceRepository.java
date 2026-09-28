package com.accentra.leavemanagement.repository;

import com.accentra.leavemanagement.entity.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {

    Optional<LeaveBalance> findByEmployeeIdAndLeaveTypeIdAndYear(Long employeeId, Long leaveTypeId, Integer year);

    List<LeaveBalance> findByLeaveTypeIdAndYear(Long leaveTypeId, Integer year);

    @Query("select b from LeaveBalance b join fetch b.leaveType where b.year = :year")
    List<LeaveBalance> findAllForYear(@Param("year") Integer year);
}
