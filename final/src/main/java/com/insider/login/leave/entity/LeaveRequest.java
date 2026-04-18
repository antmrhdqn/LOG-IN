package com.insider.login.leave.entity;

import com.insider.login.common.error.ErrorCode;
import com.insider.login.common.error.exception.BusinessException;
import com.insider.login.leave.enums.LeaveStatus;
import com.insider.login.leave.enums.LeaveType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_request")
@Getter
public class LeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "leave_request_id")
    private Long leaveRequestId;

    @Column(name = "member_id", nullable = false)
    private int memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false)
    private LeaveType leaveType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LeaveStatus status;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "use_days", nullable = false, precision = 4, scale = 1)
    private BigDecimal useDays;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt;

    @Column(name = "approver_id")
    private Integer approverId;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    protected LeaveRequest() {
    }

    @Builder
    public LeaveRequest(int memberId, LeaveType leaveType, LeaveStatus status,
                        LocalDate startDate, LocalDate endDate, BigDecimal useDays,
                        String reason, LocalDateTime appliedAt) {
        this.memberId = memberId;
        this.leaveType = leaveType;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
        this.useDays = useDays;
        this.reason = reason;
        this.appliedAt = appliedAt;
    }

    public void approve(int approverId) {
        if (!status.canTransitionTo(LeaveStatus.APPROVED)) {
            throw new BusinessException(ErrorCode.LEAVE_INVALID_STATUS_TRANSITION);
        }
        this.status = LeaveStatus.APPROVED;
        this.approverId = approverId;
        this.approvedAt = LocalDateTime.now();
    }

    public void reject(int approverId, String rejectReason) {
        if (!status.canTransitionTo(LeaveStatus.REJECTED)) {
            throw new BusinessException(ErrorCode.LEAVE_INVALID_STATUS_TRANSITION);
        }
        this.status = LeaveStatus.REJECTED;
        this.approverId = approverId;
        this.approvedAt = LocalDateTime.now();
        this.rejectReason = rejectReason;
    }

    public void cancel() {
        if (!status.canTransitionTo(LeaveStatus.CANCELLED)) {
            throw new BusinessException(ErrorCode.LEAVE_INVALID_STATUS_TRANSITION);
        }
        this.status = LeaveStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
    }

    public boolean canCancelApproved() {
        return status == LeaveStatus.APPROVED && startDate.isAfter(LocalDate.now());
    }
}
