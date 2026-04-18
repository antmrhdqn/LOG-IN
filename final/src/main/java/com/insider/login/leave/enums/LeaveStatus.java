package com.insider.login.leave.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LeaveStatus {

    PENDING("대기"),
    APPROVED("승인"),
    REJECTED("반려"),
    CANCELLED("취소");

    private final String description;

    public boolean canTransitionTo(LeaveStatus target) {
        return switch (this) {
            case PENDING -> target == APPROVED || target == REJECTED || target == CANCELLED;
            case APPROVED -> target == CANCELLED;
            case REJECTED, CANCELLED -> false;
        };
    }
}
