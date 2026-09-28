package com.accentra.leavemanagement.config;

import com.accentra.leavemanagement.entity.WorkflowSettings;
import com.accentra.leavemanagement.repository.WorkflowSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Creates the workflow settings row from externalised configuration on first start. Afterwards the
 * database value is authoritative and HR changes it through the API.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class WorkflowSettingsInitializer implements ApplicationRunner {

    private final WorkflowSettingsRepository repository;
    private final AppProperties properties;
    private final Clock clock;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (repository.existsById(WorkflowSettings.SINGLETON_ID)) {
            return;
        }
        WorkflowSettings settings = new WorkflowSettings();
        settings.setEscalationTimeoutMinutes(properties.workflow().defaultEscalationTimeoutMinutes());
        settings.setTeamAbsenceThresholdPercent(properties.workflow().defaultTeamAbsenceThresholdPercent());
        settings.setUpdatedAt(clock.instant());
        repository.save(settings);
        log.info("Initialised workflow settings: escalation timeout {} min, team absence threshold {}%",
                settings.getEscalationTimeoutMinutes(), settings.getTeamAbsenceThresholdPercent());
    }
}
