package com.insider.login.common.error;

import com.insider.login.common.response.ResponseMessage; // 변경된 패키지 임포트
import com.insider.login.common.error.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ResponseMessage<String>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();

        return new ResponseEntity<>(
                ResponseMessage.error(errorCode.getStatus(), errorCode.getMessage(), errorCode.getCode()),
                HttpStatus.valueOf(errorCode.getStatus())
        );
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ResponseMessage<String>> handleException(Exception e) {
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;

        return new ResponseEntity<>(
                ResponseMessage.error(
                        errorCode.getStatus(),
                        errorCode.getMessage(),
                        errorCode.getCode()
                ),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}