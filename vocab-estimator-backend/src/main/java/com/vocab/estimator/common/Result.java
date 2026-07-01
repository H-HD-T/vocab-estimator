package com.vocab.estimator.common;

public class Result<T> {
    private Integer code;
    private String message;
    private T data;

    public Result() {}

    public Integer getCode() { return code; }
    public void setCode(Integer code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code = 200; r.message = "Success"; r.data = data;
        return r;
    }

    public static <T> Result<T> error(Integer code, String message) {
        Result<T> r = new Result<>();
        r.code = code; r.message = message;
        return r;
    }

    public static <T> Result<T> error(String message) {
        return error(500, message);
    }
}