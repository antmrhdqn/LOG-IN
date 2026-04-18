package com.insider.login.leave.util;

import com.insider.login.common.error.ErrorCode;
import com.insider.login.common.error.exception.BusinessException;
import com.insider.login.leave.enums.LeaveType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

@Component
public class LeaveDaysCalculator {

    public BigDecimal calculateUseDays(LeaveType type, LocalDate startDate, LocalDate endDate) {
        if (type.isHalfDay()) {
            return new BigDecimal("0.5");
        }

        long workingDays = startDate.datesUntil(endDate.plusDays(1))
                .filter(date -> {
                    DayOfWeek dow = date.getDayOfWeek();
                    return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
                })
                .count();

        return BigDecimal.valueOf(workingDays);
    }

    public void validateDateRange(LeaveType type, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (endDate.isBefore(startDate)) {
            throw new BusinessException(ErrorCode.INVALID_LEAVE_PERIOD);
        }
        if (type.isHalfDay() && !startDate.equals(endDate)) {
            throw new BusinessException(ErrorCode.LEAVE_HALF_DAY_DATE_MISMATCH);
        }
        if (startDate.isBefore(LocalDate.now())) {
            throw new BusinessException(ErrorCode.LEAVE_PAST_DATE);
        }
    }
}
