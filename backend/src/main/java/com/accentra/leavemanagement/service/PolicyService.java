package com.accentra.leavemanagement.service;

import com.accentra.leavemanagement.dto.CreatePolicyRequest;
import com.accentra.leavemanagement.dto.LeavePolicyResponse;
import com.accentra.leavemanagement.dto.UpdatePolicyRequest;
import com.accentra.leavemanagement.dto.UpdateSettingsRequest;
import com.accentra.leavemanagement.dto.WorkflowSettingsResponse;
import com.accentra.leavemanagement.entity.LeavePolicy;
import com.accentra.leavemanagement.entity.LeaveType;
import com.accentra.leavemanagement.entity.WorkflowSettings;
import com.accentra.leavemanagement.enums.AuditAction;
import com.accentra.leavemanagement.exception.DuplicateResourceException;
import com.accentra.leavemanagement.exception.ResourceNotFoundException;
import com.accentra.leavemanagement.repository.LeavePolicyRepository;
import com.accentra.leavemanagement.repository.LeaveTypeRepository;
import com.accentra.leavemanagement.repository.WorkflowSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/** Database-driven leave policies and organisation workflow settings (threshold, escalation timeout). */
@Service
@RequiredArgsConstructor
public class PolicyService {

    private final LeavePolicyRepository policyRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final WorkflowSettingsRepository settingsRepository;
    private final LeaveBalanceService balanceService;
    private final AuditService auditService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<LeavePolicyResponse> listPolicies() {
        return policyRepository.findAllWithType().stream().map(LeavePolicyResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public LeavePolicy policyForType(Long leaveTypeId) {
        return policyRepository.findByLeaveTypeId(leaveTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave type not found"));
    }

    @Transactional
    public LeavePolicyResponse createPolicy(Actor actor, CreatePolicyRequest request) {
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (leaveTypeRepository.existsByCodeIgnoreCase(code)) {
            throw new DuplicateResourceException("A leave type with code " + code + " already exists");
        }
        LeaveType type = new LeaveType();
        type.setCode(code);
        type.setName(request.name().trim());
        type.setDescription(request.description());
        type.setActive(true);
        leaveTypeRepository.save(type);

        LeavePolicy policy = new LeavePolicy();
        policy.setLeaveType(type);
        policy.setAnnualEntitlement(request.annualEntitlement());
        policy.setProrated(request.prorated());
        policy.setCountWeekends(request.countWeekends());
        policy.setCountHolidays(request.countHolidays());
        policy.setUpdatedAt(clock.instant());
        policyRepository.save(policy);

        auditService.recordConfigurationChange(actor, AuditAction.POLICY_CREATED,
                "Created leave type %s (%s) with %s day(s)/year"
                        .formatted(type.getName(), code, plain(request.annualEntitlement())));
        return LeavePolicyResponse.from(policy);
    }

    @Transactional
    public LeavePolicyResponse updatePolicy(Actor actor, Long policyId, UpdatePolicyRequest request) {
        LeavePolicy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found"));
        LeaveType type = policy.getLeaveType();
        String before = describe(policy);

        type.setName(request.name().trim());
        type.setDescription(request.description());
        type.setActive(request.active());
        policy.setAnnualEntitlement(request.annualEntitlement());
        policy.setProrated(request.prorated());
        policy.setCountWeekends(request.countWeekends());
        policy.setCountHolidays(request.countHolidays());
        policy.setUpdatedAt(clock.instant());

        int currentYear = LocalDate.now(clock).getYear();
        int reallocated = balanceService.reallocate(policy, currentYear);
        auditService.recordConfigurationChange(actor, AuditAction.POLICY_UPDATED,
                "%s: %s → %s. Re-allocated %d balance(s) for %d."
                        .formatted(type.getName(), before, describe(policy), reallocated, currentYear));
        return LeavePolicyResponse.from(policy);
    }

    @Transactional(readOnly = true)
    public WorkflowSettings settings() {
        return settingsRepository.findById(WorkflowSettings.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("Workflow settings have not been initialised"));
    }

    @Transactional(readOnly = true)
    public WorkflowSettingsResponse getSettings() {
        return WorkflowSettingsResponse.from(settings());
    }

    @Transactional(readOnly = true)
    public BigDecimal teamAbsenceThresholdPercent() {
        return settings().getTeamAbsenceThresholdPercent();
    }

    @Transactional(readOnly = true)
    public int escalationTimeoutMinutes() {
        return settings().getEscalationTimeoutMinutes();
    }

    @Transactional
    public WorkflowSettingsResponse updateSettings(Actor actor, UpdateSettingsRequest request) {
        WorkflowSettings settings = settings();
        String before = "timeout %d min, threshold %s%%".formatted(settings.getEscalationTimeoutMinutes(),
                plain(settings.getTeamAbsenceThresholdPercent()));
        settings.setEscalationTimeoutMinutes(request.escalationTimeoutMinutes());
        settings.setTeamAbsenceThresholdPercent(request.teamAbsenceThresholdPercent().setScale(2, RoundingMode.HALF_UP));
        settings.setUpdatedAt(clock.instant());
        settingsRepository.save(settings);
        auditService.recordConfigurationChange(actor, AuditAction.SETTINGS_UPDATED,
                "Workflow settings: %s → timeout %d min, threshold %s%%".formatted(before,
                        settings.getEscalationTimeoutMinutes(), plain(settings.getTeamAbsenceThresholdPercent())));
        return WorkflowSettingsResponse.from(settings);
    }

    private static String describe(LeavePolicy policy) {
        return "%s day(s)/year%s%s%s%s".formatted(plain(policy.getAnnualEntitlement()),
                policy.isProrated() ? ", pro-rated" : "",
                policy.isCountWeekends() ? ", counts weekends" : "",
                policy.isCountHolidays() ? ", counts holidays" : "",
                policy.getLeaveType().isActive() ? "" : ", inactive");
    }

    private static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
