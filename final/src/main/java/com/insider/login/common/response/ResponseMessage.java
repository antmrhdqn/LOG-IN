package com.insider.login.common.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ResponseMessage<T> {
    private int status;
    private String message;
    private T data;

    public static <T> ResponseMessage<T> success(String message, T data) {
        return new ResponseMessage<>(200, message, data);
    }

}