package com.insider.login.leave.repository;

import com.insider.login.leave.entity.LeaveCalendarMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LeaveCalendarMappingRepository extends JpaRepository<LeaveCalendarMapping, Long> {

    Optional<LeaveCalendarMapping> findByLeaveRequestId(Long leaveRequestId);
}
