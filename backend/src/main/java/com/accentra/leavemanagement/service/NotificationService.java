package com.accentra.leavemanagement.service;

import com.accentra.leavemanagement.dto.NotificationResponse;
import com.accentra.leavemanagement.dto.NotificationSummaryResponse;
import com.accentra.leavemanagement.dto.TeamConflictAnalysis;
import com.accentra.leavemanagement.entity.LeaveRequest;
import com.accentra.leavemanagement.entity.Notification;
import com.accentra.leavemanagement.entity.User;
import com.accentra.leavemanagement.enums.LeaveAction;
import com.accentra.leavemanagement.enums.NotificationType;
import com.accentra.leavemanagement.enums.Role;
import com.accentra.leavemanagement.exception.ResourceNotFoundException;
import com.accentra.leavemanagement.repository.NotificationRepository;
import com.accentra.leavemanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Decides who is told what about leave events, and hands the messages to every delivery channel. */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy");
    private static final int MAX_LIST = 50;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final List<NotificationChannel> channels;
    private final Clock clock;

    @Transactional
    public void onLeaveSubmitted(LeaveRequest request, TeamConflictAnalysis conflicts) {
        String employee = request.getEmployee().getFullName();
        send(request.getEmployee().getUser(), NotificationType.LEAVE_SUBMITTED, "Leave request submitted",
                "Your %s request for %s is awaiting approval from %s."
                        .formatted(typeName(request), describe(request), request.getApprover().getFullName()),
                request);
        send(request.getApprover().getUser(), NotificationType.NEW_LEAVE_REQUEST, "New leave request",
                "%s requested %s for %s.".formatted(employee, typeName(request), describe(request)), request);
        if (conflicts.teamLeaveWarning() || conflicts.hasConflict()) {
            String message = conflicts.teamLeaveWarning()
                    ? conflicts.warningMessage()
                    : "%s's leave overlaps with %d other team member(s)."
                            .formatted(employee, conflicts.overlappingLeaves().size());
            send(request.getApprover().getUser(), NotificationType.TEAM_CONFLICT_WARNING,
                    conflicts.teamLeaveWarning() ? "High team absence warning" : "Team leave overlap", message, request);
        }
    }

    @Transactional
    public void onTransition(LeaveRequest request, LeaveAction action, Actor actor, String comment) {
        User employeeUser = request.getEmployee().getUser();
        User managerUser = request.getApprover().getUser();
        String employee = request.getEmployee().getFullName();
        String summary = "%s for %s".formatted(typeName(request), describe(request));
        String reasonSuffix = StringUtils.hasText(comment) ? " Comment: " + comment : "";

        switch (action) {
            case MANAGER_APPROVE -> {
                send(employeeUser, NotificationType.MANAGER_APPROVED, "Manager approved your leave",
                        "%s approved your %s. It now awaits HR approval.".formatted(actor.name(), summary), request);
                notifyHr(NotificationType.AWAITING_HR_APPROVAL, "Leave awaiting HR approval",
                        "%s's %s was approved by %s and needs HR approval."
                                .formatted(employee, summary, actor.name()), request);
            }
            case HR_APPROVE -> send(employeeUser, NotificationType.HR_APPROVED, "Leave approved",
                    "Your %s has been fully approved.%s".formatted(summary, reasonSuffix), request);
            case MANAGER_REJECT, HR_REJECT -> send(employeeUser, NotificationType.LEAVE_REJECTED, "Leave rejected",
                    "Your %s was rejected by %s.%s".formatted(summary, actor.name(), reasonSuffix), request);
            case ESCALATE -> {
                notifyHr(NotificationType.LEAVE_ESCALATED, "Leave request escalated",
                        "%s's %s received no manager decision in time and was escalated to HR."
                                .formatted(employee, summary), request);
                send(managerUser, NotificationType.LEAVE_ESCALATED, "Request escalated to HR",
                        "%s's %s was escalated to HR because it was not reviewed in time."
                                .formatted(employee, summary), request);
                send(employeeUser, NotificationType.LEAVE_ESCALATED, "Request escalated",
                        "Your %s has been escalated to HR for a decision.".formatted(summary), request);
            }
            case WITHDRAW -> send(managerUser, NotificationType.LEAVE_WITHDRAWN, "Leave request withdrawn",
                    "%s withdrew their %s.".formatted(employee, summary), request);
            case REQUEST_CANCELLATION -> send(managerUser, NotificationType.CANCELLATION_REQUESTED,
                    "Cancellation requested",
                    "%s asked to cancel their approved %s.%s".formatted(employee, summary, reasonSuffix), request);
            case APPROVE_CANCELLATION -> send(employeeUser, NotificationType.CANCELLATION_APPROVED,
                    "Cancellation approved",
                    "Your %s was cancelled and %d day(s) returned to your balance."
                            .formatted(summary, request.getDays()), request);
            case REJECT_CANCELLATION -> send(employeeUser, NotificationType.CANCELLATION_REJECTED,
                    "Cancellation declined",
                    "Your request to cancel %s was declined; the leave remains approved.%s"
                            .formatted(summary, reasonSuffix), request);
            case SUBMIT -> {
                // Submission notifications need conflict data and are sent by onLeaveSubmitted.
            }
        }
    }

    @Transactional(readOnly = true)
    public NotificationSummaryResponse listFor(Long userId) {
        List<NotificationResponse> items = notificationRepository
                .findByRecipientIdOrderByCreatedAtDescIdDesc(userId, PageRequest.of(0, MAX_LIST)).stream()
                .map(NotificationResponse::from)
                .toList();
        return new NotificationSummaryResponse(notificationRepository.countByRecipientIdAndReadFalse(userId), items);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepository.countByRecipientIdAndReadFalse(userId);
    }

    @Transactional
    public NotificationResponse markRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndRecipientId(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        notification.setRead(true);
        return NotificationResponse.from(notification);
    }

    @Transactional
    public int markAllRead(Long userId) {
        return notificationRepository.markAllRead(userId);
    }

    private void notifyHr(NotificationType type, String title, String message, LeaveRequest request) {
        userRepository.findAllByRoleAndEnabledTrue(Role.HR).forEach(hr -> send(hr, type, title, message, request));
    }

    private void send(User recipient, NotificationType type, String title, String message, LeaveRequest request) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setLeaveRequest(request);
        notification.setCreatedAt(clock.instant());
        channels.forEach(channel -> channel.deliver(notification));
    }

    private static String typeName(LeaveRequest request) {
        return request.getLeaveType().getName().toLowerCase();
    }

    private static String describe(LeaveRequest request) {
        String range = request.getStartDate().equals(request.getEndDate())
                ? DATE.format(request.getStartDate())
                : DATE.format(request.getStartDate()) + " – " + DATE.format(request.getEndDate());
        return "%s (%d day%s)".formatted(range, request.getDays(), request.getDays() == 1 ? "" : "s");
    }
}
