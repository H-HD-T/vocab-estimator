package com.vocab.estimator.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("System error: ", e);
                String msg = "[" + e.getClass().getSimpleName() + "] ";
        msg += e.getMessage() != null ? e.getMessage() : "";
        Throwable cause = e.getCause();
        if (cause != null) {
            msg += " => " + cause.getClass().getSimpleName() + ": " + (cause.getMessage() != null ? cause.getMessage() : "");
        }
        return Result.error(msg);
    }
}
