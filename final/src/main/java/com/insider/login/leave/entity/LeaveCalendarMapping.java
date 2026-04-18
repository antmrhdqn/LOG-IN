package com.insider.login.leave.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "leave_calendar_mapping")
@Getter
@NoArgsConstructor(access = PROTECTED)
@AllArgsConstructor
public class LeaveCalendarMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "leave_request_id", nullable = false)
    private Long leaveRequestId;

    @Column(name = "calendar_no", nullable = false)
    private int calendarNo;
}
