package com.accentra.leavemanagement.service;

import com.accentra.leavemanagement.entity.LeaveRequest;
import com.accentra.leavemanagement.enums.ActorRole;
import com.accentra.leavemanagement.enums.LeaveAction;
import com.accentra.leavemanagement.enums.LeaveStatus;
import com.accentra.leavemanagement.exception.BusinessRuleException;
import com.accentra.leavemanagement.exception.ForbiddenException;
import com.accentra.leavemanagement.exception.InvalidStateTransitionException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.accentra.leavemanagement.enums.LeaveStatus.*;

/**
 * The only component allowed to change {@link LeaveRequest#getStatus()}.
 * <pre>
 *   SUBMIT                : (new)            → PENDING_MANAGER   by EMPLOYEE (owner)
 *   MANAGER_APPROVE       : PENDING_MANAGER  → PENDING_HR        by MANAGER (assigned approver)
 *   MANAGER_REJECT        : PENDING_MANAGER  → REJECTED          by MANAGER (assigned approver)
 *   HR_APPROVE            : PENDING_HR | ESCALATED → APPROVED    by HR
 *   HR_REJECT             : PENDING_HR | ESCALATED → REJECTED    by HR
 *   ESCALATE              : PENDING_MANAGER  → ESCALATED         by SYSTEM
 *   WITHDRAW              : PENDING_MANAGER | PENDING_HR | ESCALATED → CANCELLED by EMPLOYEE (owner)
 *   REQUEST_CANCELLATION  : APPROVED         → CANCEL_REQUESTED  by EMPLOYEE (owner, leave not started)
 *   APPROVE_CANCELLATION  : CANCEL_REQUESTED → CANCELLED         by MANAGER (assigned approver) | HR
 *   REJECT_CANCELLATION   : CANCEL_REQUESTED → APPROVED          by MANAGER (assigned approver) | HR
 * </pre>
 * Every transition validates the action, the actor's role, the current state and the business guards.
 * Rejections always require a comment.
 */
@Service
@RequiredArgsConstructor
public class LeaveStateMachineService {

    record Transition(Set<LeaveStatus> from, LeaveStatus to, Set<ActorRole> actors) {
    }

    private static final Map<LeaveAction, Transition> TRANSITIONS = new EnumMap<>(LeaveAction.class);
    private static final Set<LeaveAction> REQUIRES_COMMENT =
            EnumSet.of(LeaveAction.MANAGER_REJECT, LeaveAction.HR_REJECT, LeaveAction.REJECT_CANCELLATION);

    static {
        TRANSITIONS.put(LeaveAction.MANAGER_APPROVE, t(EnumSet.of(PENDING_MANAGER), PENDING_HR, ActorRole.MANAGER));
        TRANSITIONS.put(LeaveAction.MANAGER_REJECT, t(EnumSet.of(PENDING_MANAGER), REJECTED, ActorRole.MANAGER));
        TRANSITIONS.put(LeaveAction.HR_APPROVE, t(EnumSet.of(PENDING_HR, ESCALATED), APPROVED, ActorRole.HR));
        TRANSITIONS.put(LeaveAction.HR_REJECT, t(EnumSet.of(PENDING_HR, ESCALATED), REJECTED, ActorRole.HR));
        TRANSITIONS.put(LeaveAction.ESCALATE, t(EnumSet.of(PENDING_MANAGER), ESCALATED, ActorRole.SYSTEM));
        TRANSITIONS.put(LeaveAction.WITHDRAW,
                t(EnumSet.of(PENDING_MANAGER, PENDING_HR, ESCALATED), CANCELLED, ActorRole.EMPLOYEE));
        TRANSITIONS.put(LeaveAction.REQUEST_CANCELLATION,
                t(EnumSet.of(APPROVED), CANCEL_REQUESTED, ActorRole.EMPLOYEE));
        TRANSITIONS.put(LeaveAction.APPROVE_CANCELLATION,
                t(EnumSet.of(CANCEL_REQUESTED), CANCELLED, ActorRole.MANAGER, ActorRole.HR));
        TRANSITIONS.put(LeaveAction.REJECT_CANCELLATION,
                t(EnumSet.of(CANCEL_REQUESTED), APPROVED, ActorRole.MANAGER, ActorRole.HR));
    }

    private final Clock clock;

    /** Puts a newly created request into its initial state. */
    public void initialize(LeaveRequest request, Actor actor) {
        if (request.getStatus() != null) {
            throw new InvalidStateTransitionException("Request has already been submitted");
        }
        if (actor.role() != ActorRole.EMPLOYEE || !Objects.equals(actor.employeeId(), request.getEmployee().getId())) {
            throw new ForbiddenException("Only the employee can submit their own leave request");
        }
        request.setStatus(PENDING_MANAGER);
    }

    /**
     * Validates and applies a transition. Returns the status the request had before the transition.
     */
    public LeaveStatus transition(LeaveRequest request, LeaveAction action, Actor actor, String comment) {
        Transition transition = definition(action);
        LeaveStatus current = request.getStatus();
        if (!transition.actors().contains(actor.role())) {
            throw new ForbiddenException("Role %s cannot perform %s".formatted(actor.role(), readable(action)));
        }
        if (!transition.from().contains(current)) {
            throw new InvalidStateTransitionException("Cannot %s a request that is %s"
                    .formatted(readable(action), readable(current)));
        }
        checkGuards(request, action, actor);
        if (REQUIRES_COMMENT.contains(action) && !StringUtils.hasText(comment)) {
            throw new BusinessRuleException("COMMENT_REQUIRED", "A comment is required when rejecting");
        }
        request.setStatus(transition.to());
        return current;
    }

    /** Whether the actor could perform the action now (ignores the comment requirement). */
    public boolean isAllowed(LeaveRequest request, LeaveAction action, Actor actor) {
        Transition transition = TRANSITIONS.get(action);
        if (transition == null || !transition.actors().contains(actor.role())
                || !transition.from().contains(request.getStatus())) {
            return false;
        }
        try {
            checkGuards(request, action, actor);
            return true;
        } catch (ForbiddenException | BusinessRuleException ex) {
            return false;
        }
    }

    public Set<LeaveAction> availableActions(LeaveRequest request, Actor actor) {
        return Arrays.stream(LeaveAction.values())
                .filter(action -> action != LeaveAction.SUBMIT && action != LeaveAction.ESCALATE)
                .filter(action -> isAllowed(request, action, actor))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(LeaveAction.class)));
    }

    public LeaveStatus targetStatus(LeaveAction action) {
        return definition(action).to();
    }

    private void checkGuards(LeaveRequest request, LeaveAction action, Actor actor) {
        Long ownerId = request.getEmployee().getId();
        switch (actor.role()) {
            case EMPLOYEE -> {
                if (!Objects.equals(actor.employeeId(), ownerId)) {
                    throw new ForbiddenException("You can only change your own leave requests");
                }
            }
            case MANAGER -> {
                if (!Objects.equals(actor.employeeId(), request.getApprover().getId())) {
                    throw new ForbiddenException("Only the assigned manager can act on this request");
                }
            }
            case HR, SYSTEM -> {
                // HR acts organisation-wide; the scheduler acts on any request.
            }
        }
        if (actor.role() != ActorRole.EMPLOYEE && Objects.equals(actor.employeeId(), ownerId)) {
            throw new ForbiddenException("You cannot approve or reject your own leave");
        }
        if (action == LeaveAction.REQUEST_CANCELLATION && !request.getStartDate().isAfter(LocalDate.now(clock))) {
            throw new BusinessRuleException("CANCELLATION_NOT_ALLOWED",
                    "Only leave that has not started yet can be cancelled");
        }
    }

    private static Transition definition(LeaveAction action) {
        Transition transition = TRANSITIONS.get(action);
        if (transition == null) {
            throw new InvalidStateTransitionException(readable(action) + " is not a status transition");
        }
        return transition;
    }

    private static Transition t(Set<LeaveStatus> from, LeaveStatus to, ActorRole... actors) {
        return new Transition(Set.copyOf(from), to, Set.copyOf(Arrays.asList(actors)));
    }

    private static String readable(Enum<?> value) {
        return value.name().toLowerCase().replace('_', ' ');
    }
}
