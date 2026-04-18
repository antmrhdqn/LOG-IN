package com.insider.login.leave.dto;

import com.insider.login.leave.enums.LeaveType;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveBalanceDTO {

    private Long leaveBalanceId;
    private int memberId;
    private LeaveType leaveType;
    private BigDecimal totalDays;
    private BigDecimal usedDays;
    private BigDecimal remainingDays;
    private int year;
}
