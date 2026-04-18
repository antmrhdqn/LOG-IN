package com.insider.login.leave.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public enum LeaveType {

    ANNUAL("연차", true),
    AM_HALF("오전반차", true),
    PM_HALF("오후반차", true),
    CONDOLENCE("경조사휴가", false),
    OFFICIAL("공가", false),
    SICK("병가", false);

    private final String description;
    private final boolean deductFromAnnual;

    public boolean isHalfDay() {
        return this == AM_HALF || this == PM_HALF;
    }

    public BigDecimal getDaysPerUnit() {
        return isHalfDay() ? new BigDecimal("0.5") : BigDecimal.ONE;
    }
}
