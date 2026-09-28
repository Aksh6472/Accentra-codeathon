package com.accentra.leavemanagement.repository;

import com.accentra.leavemanagement.entity.LeavePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LeavePolicyRepository extends JpaRepository<LeavePolicy, Long> {

    Optional<LeavePolicy> findByLeaveTypeId(Long leaveTypeId);

    @Query("select p from LeavePolicy p join fetch p.leaveType t order by t.name")
    List<LeavePolicy> findAllWithType();

    @Query("select p from LeavePolicy p join fetch p.leaveType t where t.active = true order by t.name")
    List<LeavePolicy> findAllActiveWithType();
}
