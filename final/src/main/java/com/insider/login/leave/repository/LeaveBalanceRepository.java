package com.insider.login.leave.repository;

import com.insider.login.leave.entity.LeaveBalance;
import com.insider.login.leave.enums.LeaveType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {

    List<LeaveBalance> findByMemberIdAndYear(int memberId, int year);

    Optional<LeaveBalance> findByMemberIdAndLeaveTypeAndYear(int memberId, LeaveType leaveType, int year);

    List<LeaveBalance> findByMemberId(int memberId);

    Page<LeaveBalance> findAll(Pageable pageable);
}
