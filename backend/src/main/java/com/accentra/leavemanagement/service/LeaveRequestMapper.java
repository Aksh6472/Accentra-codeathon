package com.accentra.leavemanagement.service;

import com.accentra.leavemanagement.dto.LeaveRequestResponse;
import com.accentra.leavemanagement.entity.LeaveRequest;
import com.accentra.leavemanagement.entity.Team;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LeaveRequestMapper {

    private final LeaveStateMachineService stateMachine;

    public LeaveRequestResponse toResponse(LeaveRequest r, Actor viewer) {
        Team team = r.getEmployee().getTeam();
        return new LeaveRequestResponse(
                r.getId(),
                r.getEmployee().getId(),
                r.getEmployee().getFullName(),
                r.getEmployee().getEmployeeCode(),
                team != null ? team.getId() : null,
                team != null ? team.getName() : null,
                r.getLeaveType().getId(),
                r.getLeaveType().getCode(),
                r.getLeaveType().getName(),
                r.getStartDate(),
                r.getEndDate(),
                r.getDays(),
                r.getReason(),
                r.getStatus(),
                r.getTeamAbsencePercent(),
                r.isTeamLeaveWarning(),
                r.isHasTeamConflict(),
                r.getApprover().getId(),
                r.getApprover().getFullName(),
                r.getCreatedAt(),
                r.getUpdatedAt(),
                r.getEscalatedAt(),
                stateMachine.availableActions(r, viewer));
    }
}
