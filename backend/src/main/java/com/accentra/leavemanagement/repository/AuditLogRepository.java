package com.accentra.leavemanagement.repository;

import com.accentra.leavemanagement.entity.AuditLog;
import com.accentra.leavemanagement.enums.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByLeaveRequestIdOrderByCreatedAtAscIdAsc(Long leaveRequestId);

    Page<AuditLog> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

    Page<AuditLog> findByActionOrderByCreatedAtDescIdDesc(AuditAction action, Pageable pageable);

    boolean existsByLeaveRequestIdAndAction(Long leaveRequestId, AuditAction action);
}
