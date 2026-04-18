package com.insider.login.leave.util;

import com.insider.login.common.error.ErrorCode;
import com.insider.login.common.error.exception.BusinessException;
import com.insider.login.leave.enums.LeaveType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LeaveDaysCalculatorTest {

    private LeaveDaysCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new LeaveDaysCalculator();
    }

    @Test
    @DisplayName("월~금 5일 신청 시 영업일 5일 반환")
    void calculateUseDays_weekdays_returns5() {
        // 다음 주 월~금 (미래 날짜)
        LocalDate monday = LocalDate.now().plusWeeks(1).with(java.time.DayOfWeek.MONDAY);
        LocalDate friday = monday.plusDays(4);

        BigDecimal result = calculator.calculateUseDays(LeaveType.ANNUAL, monday, friday);

        assertThat(result).isEqualByComparingTo(new BigDecimal("5"));
    }

    @Test
    @DisplayName("월~일 7일 신청 시 주말 제외하여 5일 반환")
    void calculateUseDays_fullWeek_returns5() {
        LocalDate monday = LocalDate.now().plusWeeks(1).with(java.time.DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);

        BigDecimal result = calculator.calculateUseDays(LeaveType.ANNUAL, monday, sunday);

        assertThat(result).isEqualByComparingTo(new BigDecimal("5"));
    }

    @Test
    @DisplayName("오전반차 신청 시 0.5일 반환")
    void calculateUseDays_amHalf_returnsHalf() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        BigDecimal result = calculator.calculateUseDays(LeaveType.AM_HALF, tomorrow, tomorrow);

        assertThat(result).isEqualByComparingTo(new BigDecimal("0.5"));
    }

    @Test
    @DisplayName("오후반차 신청 시 0.5일 반환")
    void calculateUseDays_pmHalf_returnsHalf() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        BigDecimal result = calculator.calculateUseDays(LeaveType.PM_HALF, tomorrow, tomorrow);

        assertThat(result).isEqualByComparingTo(new BigDecimal("0.5"));
    }

    @Test
    @DisplayName("종료일이 시작일보다 빠르면 INVALID_LEAVE_PERIOD 예외 발생")
    void validateDateRange_endBeforeStart_throwsException() {
        LocalDate start = LocalDate.now().plusDays(5);
        LocalDate end = LocalDate.now().plusDays(3);

        assertThatThrownBy(() -> calculator.validateDateRange(LeaveType.ANNUAL, start, end))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_LEAVE_PERIOD));
    }

    @Test
    @DisplayName("반차인데 시작일과 종료일이 다르면 LEAVE_HALF_DAY_DATE_MISMATCH 예외 발생")
    void validateDateRange_halfDayWithDifferentDates_throwsException() {
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = LocalDate.now().plusDays(2);

        assertThatThrownBy(() -> calculator.validateDateRange(LeaveType.AM_HALF, start, end))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.LEAVE_HALF_DAY_DATE_MISMATCH));
    }

    @Test
    @DisplayName("과거 날짜 신청 시 LEAVE_PAST_DATE 예외 발생")
    void validateDateRange_pastDate_throwsException() {
        LocalDate past = LocalDate.now().minusDays(1);

        assertThatThrownBy(() -> calculator.validateDateRange(LeaveType.ANNUAL, past, past))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.LEAVE_PAST_DATE));
    }

    @Test
    @DisplayName("null 날짜 전달 시 INVALID_INPUT_VALUE 예외 발생")
    void validateDateRange_nullDate_throwsException() {
        assertThatThrownBy(() -> calculator.validateDateRange(LeaveType.ANNUAL, null, null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }
}
