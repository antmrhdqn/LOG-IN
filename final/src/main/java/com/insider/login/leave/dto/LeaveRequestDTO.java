package com.insider.login.leave.dto;

import com.insider.login.leave.enums.LeaveStatus;
import com.insider.login.leave.enums.LeaveType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveRequestDTO {

    private Long leaveRequestId;
    private int memberId;
    private String memberName;
    private LeaveType leaveType;
    private LeaveStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal useDays;
    private String reason;
    private LocalDateTime appliedAt;
    private Integer approverId;
    private String approverName;
    private LocalDateTime approvedAt;
    private String rejectReason;
}
