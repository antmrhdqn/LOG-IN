package com.insider.login.leave.repository;

import com.insider.login.leave.entity.LeaveAccrualHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaveAccrualHistoryRepository extends JpaRepository<LeaveAccrualHistory, Long> {

    Page<LeaveAccrualHistory> findAll(Pageable pageable);

    List<LeaveAccrualHistory> findByMemberId(int memberId);
}
