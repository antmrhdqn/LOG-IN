package com.insider.login.common.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ResponseMessage<T> {
    private int status;
    private String message;
    private T data;

    // 성공 응답을 위한 정적 메서드
    public static <T> ResponseMessage<T> success(String message, T data) {
        return new ResponseMessage<>(200, message, data);
    }

    // 에러 응답을 위한 정적 메서드 (ErrorCode 객체를 직접 받음)
    public static ResponseMessage<String> error(int status, String message, String errorCode) {
        // 에러 발생 시에는 data 부분에 errorCode 문자열을 담아 보낼 수 있음
        return new ResponseMessage<>(status, message, errorCode);
    }
}