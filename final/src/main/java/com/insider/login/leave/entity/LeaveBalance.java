package com.insider.login.leave.entity;

import com.insider.login.leave.enums.LeaveType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "leave_balance")
@Getter
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "leave_balance_id")
    private Long leaveBalanceId;

    @Column(name = "member_id", nullable = false)
    private int memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false)
    private LeaveType leaveType;

    @Column(name = "total_days", nullable = false, precision = 4, scale = 1)
    private BigDecimal totalDays;

    @Column(name = "used_days", nullable = false, precision = 4, scale = 1)
    private BigDecimal usedDays;

    @Column(name = "remaining_days", nullable = false, precision = 4, scale = 1)
    private BigDecimal remainingDays;

    @Column(name = "year", nullable = false)
    private int year;

    @Column(name = "accrual_date", nullable = false)
    private LocalDate accrualDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    protected LeaveBalance() {
    }

    @Builder
    public LeaveBalance(int memberId, LeaveType leaveType, BigDecimal totalDays,
                        BigDecimal usedDays, BigDecimal remainingDays,
                        int year, LocalDate accrualDate, LocalDate expiryDate) {
        this.memberId = memberId;
        this.leaveType = leaveType;
        this.totalDays = totalDays;
        this.usedDays = usedDays;
        this.remainingDays = remainingDays;
        this.year = year;
        this.accrualDate = accrualDate;
        this.expiryDate = expiryDate;
    }

    public void deduct(BigDecimal days) {
        if (remainingDays.compareTo(days) < 0) {
            throw new IllegalStateException("잔여일수 부족");
        }
        this.usedDays = usedDays.add(days);
        this.remainingDays = remainingDays.subtract(days);
    }

    public void restore(BigDecimal days) {
        BigDecimal newUsedDays = usedDays.subtract(days);
        if (newUsedDays.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("복원 일수 오류");
        }
        this.usedDays = newUsedDays;
        this.remainingDays = remainingDays.add(days);
    }

    public void increase(BigDecimal days) {
        this.totalDays = totalDays.add(days);
        this.remainingDays = remainingDays.add(days);
    }

    public boolean isExpired() {
        return expiryDate != null && LocalDate.now().isAfter(expiryDate);
    }
}
