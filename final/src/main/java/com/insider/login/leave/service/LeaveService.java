package com.insider.login.leave.service;

import com.insider.login.calendar.entity.Calendar;
import com.insider.login.calendar.repository.CalendarRepository;
import com.insider.login.common.error.ErrorCode;
import com.insider.login.common.error.exception.BusinessException;
import com.insider.login.leave.dto.LeaveAccrualDTO;
import com.insider.login.leave.dto.LeaveRequestDTO;
import com.insider.login.leave.entity.LeaveAccrualHistory;
import com.insider.login.leave.entity.LeaveCalendarMapping;
import com.insider.login.leave.entity.LeaveRequest;
import com.insider.login.leave.enums.LeaveStatus;
import com.insider.login.leave.enums.LeaveType;
import com.insider.login.leave.repository.*;
import com.insider.login.leave.util.LeaveDaysCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceService leaveBalanceService;
    private final LeaveCalendarMappingRepository leaveCalendarMappingRepository;
    private final LeaveAccrualHistoryRepository leaveAccrualHistoryRepository;
    private final LeaveDaysCalculator leaveDaysCalculator;
    private final CalendarRepository calendarRepository;
    private final LeaveMemberRepository leaveMemberRepository;
    private final LeavePositionRepository leavePositionRepository;
    private final LeaveDepartmentRepository leaveDepartmentRepository;

    @Transactional
    public LeaveRequestDTO createLeaveRequest(LeaveRequestDTO dto) {
        leaveDaysCalculator.validateDateRange(dto.getLeaveType(), dto.getStartDate(), dto.getEndDate());

        BigDecimal useDays = leaveDaysCalculator.calculateUseDays(dto.getLeaveType(), dto.getStartDate(), dto.getEndDate());

        boolean overlaps = leaveRequestRepository.hasOverlappingRequest(
                dto.getMemberId(),
                List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED),
                dto.getStartDate(),
                dto.getEndDate()
        );
        if (overlaps) {
            throw new BusinessException(ErrorCode.LEAVE_DUPLICATE_DATE);
        }

        LeaveType targetType = dto.getLeaveType().isDeductFromAnnual() ? LeaveType.ANNUAL : dto.getLeaveType();
        var balance = leaveBalanceService.getBalance(dto.getMemberId(), targetType, LocalDate.now().getYear());
        if (balance.getRemainingDays().compareTo(useDays) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_LEAVE_DAYS);
        }

        LeaveRequest request = LeaveRequest.builder()
                .memberId(dto.getMemberId())
                .leaveType(dto.getLeaveType())
                .status(LeaveStatus.PENDING)
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .useDays(useDays)
                .reason(dto.getReason())
                .appliedAt(LocalDateTime.now())
                .build();

        LeaveRequest saved = leaveRequestRepository.save(request);
        return toDto(saved, null);
    }

    @Transactional(readOnly = true)
    public Page<LeaveRequestDTO> selectPendingRequests(Pageable pageable) {
        return leaveRequestRepository.findByStatus(LeaveStatus.PENDING, pageable)
                .map(request -> {
                    String name = leaveMemberRepository.findNameByMemberId(request.getMemberId());
                    return toDto(request, name);
                });
    }

    @Transactional(readOnly = true)
    public Page<LeaveRequestDTO> selectMyRequests(int memberId, Pageable pageable) {
        return leaveRequestRepository.findByMemberId(memberId, pageable)
                .map(request -> toDto(request, null));
    }

    @Transactional
    public void approveRequest(Long requestId, int approverId) {
        LeaveRequest request = findRequestOrThrow(requestId);

        request.approve(approverId);

        leaveBalanceService.deductDays(request.getMemberId(), request.getLeaveType(), request.getUseDays());

        Calendar calendar = createCalendarEvent(request);
        Calendar savedCalendar = calendarRepository.save(calendar);

        leaveCalendarMappingRepository.save(
                new LeaveCalendarMapping(null, request.getLeaveRequestId(), savedCalendar.getCalendarNo())
        );
    }

    @Transactional
    public void rejectRequest(Long requestId, int approverId, String rejectReason) {
        LeaveRequest request = findRequestOrThrow(requestId);
        request.reject(approverId, rejectReason);
    }

    @Transactional
    public void cancelRequest(Long requestId) {
        LeaveRequest request = findRequestOrThrow(requestId);

        if (request.getStatus() == LeaveStatus.PENDING) {
            request.cancel();
        } else if (request.getStatus() == LeaveStatus.APPROVED) {
            if (!request.canCancelApproved()) {
                throw new BusinessException(ErrorCode.LEAVE_CANCEL_AFTER_START);
            }
            request.cancel();
            leaveBalanceService.restoreDays(request.getMemberId(), request.getLeaveType(), request.getUseDays());
            leaveCalendarMappingRepository.findByLeaveRequestId(requestId)
                    .ifPresent(mapping -> {
                        calendarRepository.deleteById(mapping.getCalendarNo());
                        leaveCalendarMappingRepository.delete(mapping);
                    });
        } else {
            throw new BusinessException(ErrorCode.LEAVE_ALREADY_FINISHED);
        }
    }

    @Transactional
    public void grantSpecialLeave(LeaveAccrualDTO dto, int grantorId) {
        leaveBalanceService.grantLeave(dto.getMemberId(), dto.getLeaveType(), dto.getAccrualDays(), LocalDate.now());

        LeaveAccrualHistory history = LeaveAccrualHistory.builder()
                .memberId(dto.getMemberId())
                .leaveType(dto.getLeaveType())
                .accrualDays(dto.getAccrualDays())
                .accrualReason(dto.getAccrualReason())
                .grantedBy(grantorId)
                .grantedAt(LocalDateTime.now())
                .build();

        leaveAccrualHistoryRepository.save(history);
    }

    @Transactional(readOnly = true)
    public Page<LeaveAccrualHistory> selectAccrualHistory(Pageable pageable) {
        return leaveAccrualHistoryRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public com.insider.login.leave.dto.LeaveInfoDTO getLeaveInfoById(int memberId) {
        return leaveBalanceService.getBalanceInfo(memberId);
    }

    private Calendar createCalendarEvent(LeaveRequest request) {
        int memberId = request.getMemberId();
        String memberName = leaveMemberRepository.findNameByMemberId(memberId);
        String positionLevel = leaveMemberRepository.findPositionLevelByMemberId(memberId);
        String positionName = leavePositionRepository.findPositionNameByPositionLevel(positionLevel);
        int departNo = leaveMemberRepository.findDepartNoByMemberId(memberId);
        String departName = leaveDepartmentRepository.findById(departNo)
                .map(d -> d.getDepartName())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));

        String calendarName = departName + " " + memberName + " " + positionName + " " + request.getLeaveType().getDescription();

        LocalDateTime startDateTime;
        LocalDateTime endDateTime;

        if (request.getLeaveType() == LeaveType.AM_HALF) {
            startDateTime = request.getStartDate().atTime(LocalTime.of(9, 0));
            endDateTime = request.getStartDate().atTime(LocalTime.of(13, 0));
        } else if (request.getLeaveType() == LeaveType.PM_HALF) {
            startDateTime = request.getStartDate().atTime(LocalTime.of(14, 0));
            endDateTime = request.getStartDate().atTime(LocalTime.of(18, 0));
        } else {
            startDateTime = request.getStartDate().atStartOfDay();
            endDateTime = request.getEndDate().atTime(LocalTime.of(23, 59));
        }

        return new Calendar(
                0,
                calendarName,
                startDateTime,
                endDateTime,
                "yellow",
                departName,
                null,
                memberId
        );
    }

    private LeaveRequest findRequestOrThrow(Long requestId) {
        return leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LEAVE_NOT_FOUND));
    }

    private LeaveRequestDTO toDto(LeaveRequest request, String memberName) {
        return LeaveRequestDTO.builder()
                .leaveRequestId(request.getLeaveRequestId())
                .memberId(request.getMemberId())
                .memberName(memberName)
                .leaveType(request.getLeaveType())
                .status(request.getStatus())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .useDays(request.getUseDays())
                .reason(request.getReason())
                .appliedAt(request.getAppliedAt())
                .approverId(request.getApproverId())
                .approvedAt(request.getApprovedAt())
                .rejectReason(request.getRejectReason())
                .build();
    }
}
