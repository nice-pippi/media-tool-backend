package com.pippi.mediatool.application.exception;

/**
 * @Author: hong
 * @CreateTime: 2026-02-12
 * @Description: 业务异常
 * @Version: 1.0
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

}
