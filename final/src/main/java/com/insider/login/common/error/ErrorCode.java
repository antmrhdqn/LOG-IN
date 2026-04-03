package com.insider.login.common.error;

import lombok.Getter;

@Getter
public enum ErrorCode {
    // 공통 에러
    INVALID_INPUT_VALUE(400, "C001", "잘못된 입력값입니다."),
    METHOD_NOT_ALLOWED(405, "C002", "허용되지 않은 메소드입니다."),
    MISSING_PARAMETER(400, "C003", "필수 파라미터가 누락되었습니다."),
    NOT_FOUND_ENDPOINT(404, "C004", "존재하지 않는 API 경로입니다."),
    HANDLE_ACCESS_DENIED(403, "C005", "접근 권한이 없습니다."),
    INVALID_TYPE(400, "C006", "잘못된 타입이 입력되었습니다."),
    UNSUPPORTED_MEDIA_TYPE(415, "C007", "지원하지 않는 미디어 타입입니다."),
    DATABASE_ERROR(500, "C008", "데이터베이스 오류가 발생했습니다."),
    INTERNAL_SERVER_ERROR(500, "C999", "서버 내부 오류가 발생했습니다."),


    // 회원(Member) 관련 에러
    MEMBER_NOT_FOUND(404, "M001", "존재하지 않는 회원입니다."),
    EXPIRED_TOKEN(401, "M002", "만료된 토큰입니다."),
    INVALID_TOKEN(401, "M003", "유효하지 않은 토큰입니다."),
    UNSUPPORTED_TOKEN(401, "M004", "지원하지 않는 토큰 방식입니다."),
    PASSWORD_MISMATCH(400, "M005", "비밀번호가 일치하지 않습니다."),
    ACCOUNT_DELETED(403, "M006", "탈퇴한 계정입니다."),
    EMAIL_NOT_VERIFIED(403, "M007", "이메일 인증이 완료되지 않았습니다."),

    // 휴가(leave) 관련 에러
    INSUFFICIENT_LEAVE_DAYS(400, "L001", "잔여 휴가 일수가 부족합니다."),
    INVALID_LEAVE_PERIOD(400, "L002", "종료일이 시작일보다 빠를 수 없습니다."),
    LEAVE_NOT_FOUND(404, "L003", "해당 휴가 신청 내역을 찾을 수 없습니다."),

    // 일정(Calendar) 관련 에러
    CALENDAR_NOT_FOUND(404, "CA01", "해당 일정을 찾을 수 없습니다."),
    CALENDAR_ACCESS_DENIED(403, "CA02", "해당 일정을 수정/삭제할 권한이 없습니다."),

    // 설문(Survey) 관련 에러
    ALREADY_PARTICIPATED_SURVEY(400, "S001", "이미 참여한 설문조사입니다."),
    SURVEY_NOT_FOUND(404, "S002", "해당 설문조사를 찾을 수 없습니다.");

    private final int status;
    private final String code;
    private final String message;

    ErrorCode(int status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}