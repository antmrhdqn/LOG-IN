package com.insider.login.leave.service;

import com.insider.login.common.error.ErrorCode;
import com.insider.login.common.error.exception.BusinessException;
import com.insider.login.leave.dto.LeaveInfoDTO;
import com.insider.login.leave.entity.LeaveBalance;
import com.insider.login.leave.enums.LeaveType;
import com.insider.login.leave.repository.LeaveBalanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveBalanceService {

    private final LeaveBalanceRepository leaveBalanceRepository;

    @Transactional(readOnly = true)
    public LeaveBalance getBalance(int memberId, LeaveType leaveType, int year) {
        return leaveBalanceRepository.findByMemberIdAndLeaveTypeAndYear(memberId, leaveType, year)
                .orElseThrow(() -> new BusinessException(ErrorCode.LEAVE_BALANCE_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public LeaveInfoDTO getBalanceInfo(int memberId) {
        int currentYear = LocalDate.now().getYear();
        List<LeaveBalance> balances = leaveBalanceRepository.findByMemberIdAndYear(memberId, currentYear);

        BigDecimal annualTotal = BigDecimal.ZERO;
        BigDecimal annualUsed = BigDecimal.ZERO;
        BigDecimal annualRemaining = BigDecimal.ZERO;
        BigDecimal specialTotal = BigDecimal.ZERO;
        BigDecimal specialUsed = BigDecimal.ZERO;
        BigDecimal specialRemaining = BigDecimal.ZERO;

        for (LeaveBalance balance : balances) {
            if (balance.getLeaveType() == LeaveType.ANNUAL) {
                annualTotal = annualTotal.add(balance.getTotalDays());
                annualUsed = annualUsed.add(balance.getUsedDays());
                annualRemaining = annualRemaining.add(balance.getRemainingDays());
            } else {
                specialTotal = specialTotal.add(balance.getTotalDays());
                specialUsed = specialUsed.add(balance.getUsedDays());
                specialRemaining = specialRemaining.add(balance.getRemainingDays());
            }
        }

        return LeaveInfoDTO.builder()
                .memberId(memberId)
                .annualTotal(annualTotal)
                .annualUsed(annualUsed)
                .annualRemaining(annualRemaining)
                .specialTotal(specialTotal)
                .specialUsed(specialUsed)
                .specialRemaining(specialRemaining)
                .totalDays(annualTotal.add(specialTotal))
                .totalUsed(annualUsed.add(specialUsed))
                .totalRemaining(annualRemaining.add(specialRemaining))
                .build();
    }

    @Transactional(readOnly = true)
    public Page<LeaveInfoDTO> getAllBalances(Pageable pageable) {
        return leaveBalanceRepository.findAll(pageable)
                .map(balance -> {
                    boolean isAnnual = balance.getLeaveType() == LeaveType.ANNUAL;
                    return LeaveInfoDTO.builder()
                            .memberId(balance.getMemberId())
                            .annualTotal(isAnnual ? balance.getTotalDays() : BigDecimal.ZERO)
                            .annualUsed(isAnnual ? balance.getUsedDays() : BigDecimal.ZERO)
                            .annualRemaining(isAnnual ? balance.getRemainingDays() : BigDecimal.ZERO)
                            .specialTotal(isAnnual ? BigDecimal.ZERO : balance.getTotalDays())
                            .specialUsed(isAnnual ? BigDecimal.ZERO : balance.getUsedDays())
                            .specialRemaining(isAnnual ? BigDecimal.ZERO : balance.getRemainingDays())
                            .totalDays(balance.getTotalDays())
                            .totalUsed(balance.getUsedDays())
                            .totalRemaining(balance.getRemainingDays())
                            .build();
                });
    }

    @Transactional
    public void deductDays(int memberId, LeaveType leaveType, BigDecimal days) {
        LeaveType targetType = leaveType.isDeductFromAnnual() ? LeaveType.ANNUAL : leaveType;
        int year = LocalDate.now().getYear();
        LeaveBalance balance = getBalance(memberId, targetType, year);
        balance.deduct(days);
    }

    @Transactional
    public void restoreDays(int memberId, LeaveType leaveType, BigDecimal days) {
        LeaveType targetType = leaveType.isDeductFromAnnual() ? LeaveType.ANNUAL : leaveType;
        int year = LocalDate.now().getYear();
        LeaveBalance balance = getBalance(memberId, targetType, year);
        balance.restore(days);
    }

    @Transactional
    public void grantLeave(int memberId, LeaveType leaveType, BigDecimal days, LocalDate accrualDate) {
        int year = accrualDate.getYear();

        leaveBalanceRepository.findByMemberIdAndLeaveTypeAndYear(memberId, leaveType, year)
                .ifPresentOrElse(
                        existing -> increaseBalance(existing, days),
                        () -> {
                            LocalDate expiryDate = leaveType == LeaveType.ANNUAL
                                    ? accrualDate.withDayOfYear(accrualDate.lengthOfYear())
                                    : null;

                            LeaveBalance newBalance = LeaveBalance.builder()
                                    .memberId(memberId)
                                    .leaveType(leaveType)
                                    .totalDays(days)
                                    .usedDays(BigDecimal.ZERO)
                                    .remainingDays(days)
                                    .year(year)
                                    .accrualDate(accrualDate)
                                    .expiryDate(expiryDate)
                                    .build();
                            leaveBalanceRepository.save(newBalance);
                        }
                );
    }

    private void increaseBalance(LeaveBalance balance, BigDecimal days) {
        balance.increase(days);
    }
}
