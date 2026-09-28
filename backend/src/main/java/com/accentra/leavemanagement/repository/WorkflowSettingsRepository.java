package com.accentra.leavemanagement.repository;

import com.accentra.leavemanagement.entity.WorkflowSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowSettingsRepository extends JpaRepository<WorkflowSettings, Long> {
}
