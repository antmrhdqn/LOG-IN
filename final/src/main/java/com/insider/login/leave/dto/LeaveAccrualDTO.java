package com.insider.login.leave.dto;

import com.insider.login.leave.enums.LeaveType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LeaveAccrualDTO {

    private int memberId;
    private LeaveType leaveType;
    private java.math.BigDecimal accrualDays;
    private String accrualReason;
}
