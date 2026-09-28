package com.accentra.leavemanagement.service;

import com.accentra.leavemanagement.entity.LeaveRequest;
import com.accentra.leavemanagement.entity.Team;
import com.accentra.leavemanagement.enums.ActorRole;
import com.accentra.leavemanagement.exception.ForbiddenException;
import org.springframework.stereotype.Component;

import java.util.Objects;

/** Record-level read access rules, applied on top of the URL/role rules in SecurityConfig. */
@Component
public class LeaveAccessPolicy {

    public boolean canView(LeaveRequest request, Actor actor) {
        return switch (actor.role()) {
            case HR, SYSTEM -> true;
            case MANAGER -> Objects.equals(request.getApprover().getId(), actor.employeeId())
                    || isCurrentManager(request, actor);
            case EMPLOYEE -> Objects.equals(request.getEmployee().getId(), actor.employeeId());
        };
    }

    public void assertCanView(LeaveRequest request, Actor actor) {
        if (!canView(request, actor)) {
            throw new ForbiddenException("You do not have access to this leave request");
        }
    }

    /** Team-level data (who else is away) is only for approvers. */
    public boolean canSeeTeamData(Actor actor) {
        return actor.role() == ActorRole.MANAGER || actor.role() == ActorRole.HR;
    }

    public void assertCanViewTeam(Team team, Actor actor) {
        boolean allowed = actor.role() == ActorRole.HR
                || (actor.role() == ActorRole.MANAGER && team.getManager() != null
                && Objects.equals(team.getManager().getId(), actor.employeeId()));
        if (!allowed) {
            throw new ForbiddenException("You do not have access to this team");
        }
    }

    private static boolean isCurrentManager(LeaveRequest request, Actor actor) {
        var manager = request.getEmployee().getManager();
        return manager != null && Objects.equals(manager.getId(), actor.employeeId());
    }
}
