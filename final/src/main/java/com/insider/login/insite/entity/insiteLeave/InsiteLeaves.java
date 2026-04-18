package com.insider.login.insite.entity.insiteLeave;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity(name = "insiteLeaves")
@Table(name = "leave_balance")
public class InsiteLeaves {

    @Id
    @Column(name = "leave_balance_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long leaveBalanceId;

    @Column(name = "member_id", nullable = false)
    private int memberId;

    @Column(name = "leave_type", nullable = false)
    private String leaveType;

    @Column(name = "remaining_days", nullable = false)
    private BigDecimal remainingDays;

    @Column(name = "year", nullable = false)
    private int year;

    protected InsiteLeaves() {
    }

    public Long getLeaveBalanceId() {
        return leaveBalanceId;
    }

    public int getMemberId() {
        return memberId;
    }

    public String getLeaveType() {
        return leaveType;
    }

    public BigDecimal getRemainingDays() {
        return remainingDays;
    }

    public int getYear() {
        return year;
    }
}
