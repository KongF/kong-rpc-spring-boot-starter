package com.kong.rpc.common.protocol;

/**
 * 响应状态码
 *
 * @author k
 * @since 1.0.0
 */
public enum RpcStatus {
    SUCCESS(200,"SUCCESS"),
    ERROR(500,"ERROR"),
    NOT_FOUND(404,"NOT FOUND");

    private int code;

    private String message;

    RpcStatus(int code,String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
