package com.accentra.leavemanagement.repository;

import com.accentra.leavemanagement.entity.LeaveRequest;
import com.accentra.leavemanagement.enums.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long>, JpaSpecificationExecutor<LeaveRequest> {

    @Query("""
            select r from LeaveRequest r join fetch r.leaveType
            where r.employee.id = :employeeId
            order by r.startDate desc, r.id desc""")
    List<LeaveRequest> findForEmployee(@Param("employeeId") Long employeeId);

    @Query("""
            select count(r) > 0 from LeaveRequest r
            where r.employee.id = :employeeId and r.status in :statuses
              and r.startDate <= :to and r.endDate >= :from""")
    boolean existsOverlapping(@Param("employeeId") Long employeeId,
                              @Param("from") LocalDate from,
                              @Param("to") LocalDate to,
                              @Param("statuses") Collection<LeaveStatus> statuses);

    @Query("""
            select r from LeaveRequest r join fetch r.employee e join fetch r.leaveType
            where e.team.id = :teamId and r.status in :statuses
              and r.startDate <= :to and r.endDate >= :from
            order by r.startDate, e.fullName""")
    List<LeaveRequest> findTeamLeavesOverlapping(@Param("teamId") Long teamId,
                                                 @Param("from") LocalDate from,
                                                 @Param("to") LocalDate to,
                                                 @Param("statuses") Collection<LeaveStatus> statuses);

    @Query("""
            select r from LeaveRequest r join fetch r.employee join fetch r.leaveType
            where r.approver.id = :approverId and r.status in :statuses
            order by r.createdAt""")
    List<LeaveRequest> findForApprover(@Param("approverId") Long approverId,
                                       @Param("statuses") Collection<LeaveStatus> statuses);

    @Query("""
            select r from LeaveRequest r join fetch r.employee join fetch r.leaveType
            where r.status in :statuses
            order by r.createdAt""")
    List<LeaveRequest> findByStatuses(@Param("statuses") Collection<LeaveStatus> statuses);

    @Query("select r.id from LeaveRequest r where r.status = :status and r.createdAt < :cutoff order by r.createdAt")
    List<Long> findIdsByStatusCreatedBefore(@Param("status") LeaveStatus status, @Param("cutoff") Instant cutoff);

    @Query("""
            select r from LeaveRequest r join fetch r.employee join fetch r.leaveType
            where r.startDate <= :to and r.endDate >= :from""")
    List<LeaveRequest> findOverlappingRange(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
