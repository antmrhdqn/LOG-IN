package com.insider.login.leave.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveInfoDTO {

    private int memberId;
    private String name;
    private BigDecimal annualTotal;
    private BigDecimal annualUsed;
    private BigDecimal annualRemaining;
    private BigDecimal specialTotal;
    private BigDecimal specialUsed;
    private BigDecimal specialRemaining;
    private BigDecimal totalDays;
    private BigDecimal totalUsed;
    private BigDecimal totalRemaining;
}
