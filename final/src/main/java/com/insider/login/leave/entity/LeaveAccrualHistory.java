package com.insider.login.leave.entity;

import com.insider.login.leave.enums.LeaveType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_accrual_history")
@Getter
public class LeaveAccrualHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "accrual_id")
    private Long accrualId;

    @Column(name = "member_id", nullable = false)
    private int memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false)
    private LeaveType leaveType;

    @Column(name = "accrual_days", nullable = false, precision = 4, scale = 1)
    private BigDecimal accrualDays;

    @Column(name = "accrual_reason", nullable = false, length = 300)
    private String accrualReason;

    @Column(name = "granted_by", nullable = false)
    private int grantedBy;

    @Column(name = "granted_at", nullable = false)
    private LocalDateTime grantedAt;

    protected LeaveAccrualHistory() {
    }

    @Builder
    public LeaveAccrualHistory(int memberId, LeaveType leaveType, BigDecimal accrualDays,
                               String accrualReason, int grantedBy, LocalDateTime grantedAt) {
        this.memberId = memberId;
        this.leaveType = leaveType;
        this.accrualDays = accrualDays;
        this.accrualReason = accrualReason;
        this.grantedBy = grantedBy;
        this.grantedAt = grantedAt;
    }
}
