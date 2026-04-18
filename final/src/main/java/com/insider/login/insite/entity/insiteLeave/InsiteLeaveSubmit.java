package com.insider.login.insite.entity.insiteLeave;

import com.insider.login.insite.entity.InsiteMember;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity(name = "InsiteLeaveSubmit")
@Table(name = "leave_request")
public class InsiteLeaveSubmit {

    @Id
    @Column(name = "leave_request_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long leaveRequestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", referencedColumnName = "member_id", nullable = false)
    private InsiteMember leaveMember;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "status", nullable = false)
    private String status;

    protected InsiteLeaveSubmit() {
    }

    public Long getLeaveRequestId() {
        return leaveRequestId;
    }

    public InsiteMember getLeaveMember() {
        return leaveMember;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getStatus() {
        return status;
    }
}
