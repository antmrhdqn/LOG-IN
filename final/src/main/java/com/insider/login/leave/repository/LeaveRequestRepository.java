package com.insider.login.leave.repository;

import com.insider.login.leave.entity.LeaveRequest;
import com.insider.login.leave.enums.LeaveStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    Page<LeaveRequest> findByMemberId(int memberId, Pageable pageable);

    List<LeaveRequest> findByMemberIdAndStatus(int memberId, LeaveStatus status);

    Page<LeaveRequest> findByStatus(LeaveStatus status, Pageable pageable);

    @Query("SELECT COUNT(r) > 0 FROM LeaveRequest r " +
           "WHERE r.memberId = :memberId " +
           "AND r.status IN :statuses " +
           "AND r.startDate <= :endDate " +
           "AND r.endDate >= :startDate")
    boolean hasOverlappingRequest(@Param("memberId") int memberId,
                                  @Param("statuses") List<LeaveStatus> statuses,
                                  @Param("startDate") LocalDate startDate,
                                  @Param("endDate") LocalDate endDate);
}
