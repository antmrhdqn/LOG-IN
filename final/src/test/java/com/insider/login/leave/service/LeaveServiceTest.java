package com.insider.login.leave.service;

import com.insider.login.calendar.entity.Calendar;
import com.insider.login.calendar.repository.CalendarRepository;
import com.insider.login.common.error.ErrorCode;
import com.insider.login.common.error.exception.BusinessException;
import com.insider.login.department.entity.Department;
import com.insider.login.leave.dto.LeaveAccrualDTO;
import com.insider.login.leave.dto.LeaveRequestDTO;
import com.insider.login.leave.entity.LeaveAccrualHistory;
import com.insider.login.leave.entity.LeaveBalance;
import com.insider.login.leave.entity.LeaveCalendarMapping;
import com.insider.login.leave.entity.LeaveRequest;
import com.insider.login.leave.enums.LeaveStatus;
import com.insider.login.leave.enums.LeaveType;
import com.insider.login.leave.repository.*;
import com.insider.login.leave.util.LeaveDaysCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveServiceTest {

    @Mock LeaveRequestRepository leaveRequestRepository;
    @Mock LeaveBalanceService leaveBalanceService;
    @Mock LeaveCalendarMappingRepository leaveCalendarMappingRepository;
    @Mock LeaveAccrualHistoryRepository leaveAccrualHistoryRepository;
    @Mock LeaveDaysCalculator leaveDaysCalculator;
    @Mock CalendarRepository calendarRepository;
    @Mock LeaveMemberRepository leaveMemberRepository;
    @Mock LeavePositionRepository leavePositionRepository;
    @Mock LeaveDepartmentRepository leaveDepartmentRepository;

    @InjectMocks
    LeaveService leaveService;

    // ─── 헬퍼 ─────────────────────────────────────────────────────────────────

    private LeaveRequestDTO buildRequestDto(LeaveType type, LocalDate start, LocalDate end) {
        return LeaveRequestDTO.builder()
                .memberId(1001)
                .leaveType(type)
                .startDate(start)
                .endDate(end)
                .reason("테스트 휴가")
                .build();
    }

    private LeaveBalance buildBalance(BigDecimal remaining) {
        return LeaveBalance.builder()
                .memberId(1001)
                .leaveType(LeaveType.ANNUAL)
                .totalDays(new BigDecimal("15.0"))
                .usedDays(BigDecimal.ZERO)
                .remainingDays(remaining)
                .year(LocalDate.now().getYear())
                .accrualDate(LocalDate.now())
                .build();
    }

    private LeaveRequest buildRequest(LeaveStatus status, LocalDate start, LocalDate end) {
        return LeaveRequest.builder()
                .memberId(1001)
                .leaveType(LeaveType.ANNUAL)
                .status(status)
                .startDate(start)
                .endDate(end)
                .useDays(new BigDecimal("3.0"))
                .appliedAt(LocalDateTime.now())
                .build();
    }

    private void stubCalendarCreation() {
        given(leaveMemberRepository.findNameByMemberId(anyInt())).willReturn("홍길동");
        given(leaveMemberRepository.findPositionLevelByMemberId(anyInt())).willReturn("P3");
        given(leavePositionRepository.findPositionNameByPositionLevel("P3")).willReturn("대리");
        given(leaveMemberRepository.findDepartNoByMemberId(anyInt())).willReturn(1);
        given(leaveDepartmentRepository.findById(1)).willReturn(Optional.of(new Department(1, "개발부")));
        Calendar savedCalendar = new Calendar(1, "개발부 홍길동 대리 연차",
                LocalDateTime.now(), LocalDateTime.now(), "yellow", "개발부", null, 1001);
        given(calendarRepository.save(any())).willReturn(savedCalendar);
    }

    // ─── 신청 관련 ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("정상 휴가 신청 시 PENDING 상태로 저장된다")
    void createLeaveRequest_success_savedAsPending() {
        // given
        LocalDate start = LocalDate.now().plusDays(3);
        LocalDate end = LocalDate.now().plusDays(5);
        LeaveRequestDTO dto = buildRequestDto(LeaveType.ANNUAL, start, end);
        BigDecimal useDays = new BigDecimal("3.0");

        given(leaveDaysCalculator.calculateUseDays(any(), any(), any())).willReturn(useDays);
        given(leaveRequestRepository.hasOverlappingRequest(anyInt(), anyList(), any(), any())).willReturn(false);
        given(leaveBalanceService.getBalance(anyInt(), any(), anyInt())).willReturn(buildBalance(new BigDecimal("15.0")));

        LeaveRequest savedRequest = buildRequest(LeaveStatus.PENDING, start, end);
        given(leaveRequestRepository.save(any())).willReturn(savedRequest);

        // when
        LeaveRequestDTO result = leaveService.createLeaveRequest(dto);

        // then
        assertThat(result.getStatus()).isEqualTo(LeaveStatus.PENDING);
        then(leaveDaysCalculator).should().validateDateRange(LeaveType.ANNUAL, start, end);
        then(leaveRequestRepository).should().save(any(LeaveRequest.class));
    }

    @Test
    @DisplayName("잔여일수 부족 시 INSUFFICIENT_LEAVE_DAYS 예외 발생")
    void createLeaveRequest_insufficientBalance_throwsException() {
        // given
        LocalDate start = LocalDate.now().plusDays(3);
        LocalDate end = LocalDate.now().plusDays(7);
        LeaveRequestDTO dto = buildRequestDto(LeaveType.ANNUAL, start, end);

        given(leaveDaysCalculator.calculateUseDays(any(), any(), any())).willReturn(new BigDecimal("5.0"));
        given(leaveRequestRepository.hasOverlappingRequest(anyInt(), anyList(), any(), any())).willReturn(false);
        given(leaveBalanceService.getBalance(anyInt(), any(), anyInt())).willReturn(buildBalance(new BigDecimal("3.0")));

        // when & then
        assertThatThrownBy(() -> leaveService.createLeaveRequest(dto))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INSUFFICIENT_LEAVE_DAYS));
    }

    @Test
    @DisplayName("중복 날짜 신청 시 LEAVE_DUPLICATE_DATE 예외 발생")
    void createLeaveRequest_overlappingDate_throwsException() {
        // given
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = LocalDate.now().plusDays(3);
        LeaveRequestDTO dto = buildRequestDto(LeaveType.ANNUAL, start, end);

        given(leaveDaysCalculator.calculateUseDays(any(), any(), any())).willReturn(new BigDecimal("3.0"));
        given(leaveRequestRepository.hasOverlappingRequest(anyInt(), anyList(), any(), any())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> leaveService.createLeaveRequest(dto))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.LEAVE_DUPLICATE_DATE));
    }

    @Test
    @DisplayName("과거 날짜 신청 시 LEAVE_PAST_DATE 예외 발생")
    void createLeaveRequest_pastDate_throwsException() {
        // given
        LocalDate past = LocalDate.now().minusDays(1);
        LeaveRequestDTO dto = buildRequestDto(LeaveType.ANNUAL, past, past);

        doThrow(new BusinessException(ErrorCode.LEAVE_PAST_DATE))
                .when(leaveDaysCalculator).validateDateRange(any(), any(), any());

        // when & then
        assertThatThrownBy(() -> leaveService.createLeaveRequest(dto))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.LEAVE_PAST_DATE));
    }

    @Test
    @DisplayName("반차 신청 시 시작일과 종료일이 다르면 LEAVE_HALF_DAY_DATE_MISMATCH 예외 발생")
    void createLeaveRequest_halfDayWithDifferentDates_throwsException() {
        // given
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = LocalDate.now().plusDays(2);
        LeaveRequestDTO dto = buildRequestDto(LeaveType.AM_HALF, start, end);

        doThrow(new BusinessException(ErrorCode.LEAVE_HALF_DAY_DATE_MISMATCH))
                .when(leaveDaysCalculator).validateDateRange(any(), any(), any());

        // when & then
        assertThatThrownBy(() -> leaveService.createLeaveRequest(dto))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.LEAVE_HALF_DAY_DATE_MISMATCH));
    }

    @Test
    @DisplayName("반차 신청 시 useDays가 0.5일로 저장된다")
    void createLeaveRequest_halfDay_savesHalfDayUseDays() {
        // given
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LeaveRequestDTO dto = buildRequestDto(LeaveType.AM_HALF, tomorrow, tomorrow);

        given(leaveDaysCalculator.calculateUseDays(any(), any(), any())).willReturn(new BigDecimal("0.5"));
        given(leaveRequestRepository.hasOverlappingRequest(anyInt(), anyList(), any(), any())).willReturn(false);
        given(leaveBalanceService.getBalance(anyInt(), any(), anyInt())).willReturn(buildBalance(new BigDecimal("15.0")));

        ArgumentCaptor<LeaveRequest> captor = ArgumentCaptor.forClass(LeaveRequest.class);
        LeaveRequest saved = LeaveRequest.builder()
                .memberId(1001).leaveType(LeaveType.AM_HALF).status(LeaveStatus.PENDING)
                .startDate(tomorrow).endDate(tomorrow).useDays(new BigDecimal("0.5"))
                .appliedAt(LocalDateTime.now()).build();
        given(leaveRequestRepository.save(captor.capture())).willReturn(saved);

        // when
        leaveService.createLeaveRequest(dto);

        // then
        assertThat(captor.getValue().getUseDays()).isEqualByComparingTo(new BigDecimal("0.5"));
    }

    // ─── 결재 관련 ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("승인 시 상태가 APPROVED로 변경되고 잔여일수가 차감된다")
    void approveRequest_success_statusApprovedAndDaysDeducted() {
        // given
        Long requestId = 1L;
        int approverId = 2001;
        LocalDate start = LocalDate.now().plusDays(3);
        LeaveRequest request = buildRequest(LeaveStatus.PENDING, start, start.plusDays(2));
        stubCalendarCreation();

        given(leaveRequestRepository.findById(requestId)).willReturn(Optional.of(request));
        given(leaveCalendarMappingRepository.save(any())).willReturn(new LeaveCalendarMapping(null, requestId, 1));

        // when
        leaveService.approveRequest(requestId, approverId);

        // then
        assertThat(request.getStatus()).isEqualTo(LeaveStatus.APPROVED);
        assertThat(request.getApproverId()).isEqualTo(approverId);
        then(leaveBalanceService).should().deductDays(eq(1001), eq(LeaveType.ANNUAL), any());
        then(calendarRepository).should().save(any());
    }

    @Test
    @DisplayName("반려 시 상태가 REJECTED로 변경되고 반려 사유가 기록된다")
    void rejectRequest_success_statusRejectedWithReason() {
        // given
        Long requestId = 1L;
        int approverId = 2001;
        String reason = "사유 없음";
        LeaveRequest request = buildRequest(LeaveStatus.PENDING,
                LocalDate.now().plusDays(3), LocalDate.now().plusDays(5));

        given(leaveRequestRepository.findById(requestId)).willReturn(Optional.of(request));

        // when
        leaveService.rejectRequest(requestId, approverId, reason);

        // then
        assertThat(request.getStatus()).isEqualTo(LeaveStatus.REJECTED);
        assertThat(request.getRejectReason()).isEqualTo(reason);
        then(leaveBalanceService).should(never()).deductDays(anyInt(), any(), any());
    }

    @Test
    @DisplayName("이미 승인된 건을 승인 시도하면 LEAVE_INVALID_STATUS_TRANSITION 예외 발생")
    void approveRequest_alreadyApproved_throwsException() {
        // given
        Long requestId = 1L;
        LeaveRequest request = buildRequest(LeaveStatus.APPROVED,
                LocalDate.now().plusDays(3), LocalDate.now().plusDays(5));

        given(leaveRequestRepository.findById(requestId)).willReturn(Optional.of(request));

        // when & then
        assertThatThrownBy(() -> leaveService.approveRequest(requestId, 2001))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.LEAVE_INVALID_STATUS_TRANSITION));
    }

    // ─── 취소 관련 ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PENDING 상태 취소 시 CANCELLED로 변경되고 잔여일수 변동 없음")
    void cancelRequest_pendingRequest_cancelledWithoutRestore() {
        // given
        Long requestId = 1L;
        LeaveRequest request = buildRequest(LeaveStatus.PENDING,
                LocalDate.now().plusDays(3), LocalDate.now().plusDays(5));

        given(leaveRequestRepository.findById(requestId)).willReturn(Optional.of(request));

        // when
        leaveService.cancelRequest(requestId);

        // then
        assertThat(request.getStatus()).isEqualTo(LeaveStatus.CANCELLED);
        then(leaveBalanceService).should(never()).restoreDays(anyInt(), any(), any());
    }

    @Test
    @DisplayName("APPROVED 상태 + 시작일 전 취소 시 CANCELLED로 변경되고 잔여일수가 복원된다")
    void cancelRequest_approvedBeforeStart_cancelledAndRestored() {
        // given
        Long requestId = 1L;
        LocalDate futureStart = LocalDate.now().plusDays(5);
        LeaveRequest request = buildRequest(LeaveStatus.APPROVED, futureStart, futureStart.plusDays(2));

        given(leaveRequestRepository.findById(requestId)).willReturn(Optional.of(request));
        given(leaveCalendarMappingRepository.findByLeaveRequestId(any())).willReturn(Optional.empty());

        // when
        leaveService.cancelRequest(requestId);

        // then
        assertThat(request.getStatus()).isEqualTo(LeaveStatus.CANCELLED);
        then(leaveBalanceService).should().restoreDays(eq(1001), eq(LeaveType.ANNUAL), any());
    }

    @Test
    @DisplayName("APPROVED 상태 + 시작일 이후 취소 시도 시 LEAVE_CANCEL_AFTER_START 예외 발생")
    void cancelRequest_approvedAfterStart_throwsException() {
        // given
        Long requestId = 1L;
        LocalDate pastStart = LocalDate.now().minusDays(1);
        LeaveRequest request = buildRequest(LeaveStatus.APPROVED, pastStart, LocalDate.now().plusDays(1));

        given(leaveRequestRepository.findById(requestId)).willReturn(Optional.of(request));

        // when & then
        assertThatThrownBy(() -> leaveService.cancelRequest(requestId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.LEAVE_CANCEL_AFTER_START));
    }

    @Test
    @DisplayName("REJECTED 상태 취소 시도 시 LEAVE_ALREADY_FINISHED 예외 발생")
    void cancelRequest_rejectedRequest_throwsException() {
        // given
        Long requestId = 1L;
        LeaveRequest request = buildRequest(LeaveStatus.REJECTED,
                LocalDate.now().plusDays(3), LocalDate.now().plusDays(5));

        given(leaveRequestRepository.findById(requestId)).willReturn(Optional.of(request));

        // when & then
        assertThatThrownBy(() -> leaveService.cancelRequest(requestId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.LEAVE_ALREADY_FINISHED));
    }

    // ─── 특별휴가 부여 ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("특별휴가 부여 시 LeaveBalance가 갱신되고 LeaveAccrualHistory가 기록된다")
    void grantSpecialLeave_success_balanceUpdatedAndHistoryRecorded() {
        // given
        LeaveAccrualDTO dto = new LeaveAccrualDTO();
        dto.setMemberId(1001);
        dto.setLeaveType(LeaveType.CONDOLENCE);
        dto.setAccrualDays(new BigDecimal("3.0"));
        dto.setAccrualReason("경조사");
        int grantorId = 2001;

        // when
        leaveService.grantSpecialLeave(dto, grantorId);

        // then
        then(leaveBalanceService).should().grantLeave(eq(1001), eq(LeaveType.CONDOLENCE),
                eq(new BigDecimal("3.0")), any(LocalDate.class));

        ArgumentCaptor<LeaveAccrualHistory> historyCaptor = ArgumentCaptor.forClass(LeaveAccrualHistory.class);
        then(leaveAccrualHistoryRepository).should().save(historyCaptor.capture());
        LeaveAccrualHistory saved = historyCaptor.getValue();
        assertThat(saved.getMemberId()).isEqualTo(1001);
        assertThat(saved.getGrantedBy()).isEqualTo(grantorId);
        assertThat(saved.getAccrualReason()).isEqualTo("경조사");
    }
}
