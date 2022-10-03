package com.kong.rpc.common.protocol;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 响应信息封装
 *
 * @author k
 * @since 1.0.0
 */
public class RpcResponse implements Serializable {
    private String requestId;
    private RpcStatus status;

    private Map<String,String> headers = new HashMap<>();

    private Object returnValue;

    private Exception exception;

    public RpcResponse(RpcStatus status) {
        this.status = status;
    }

    public RpcStatus getStatus() {
        return status;
    }

    public void setStatus(RpcStatus status) {
        this.status = status;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Object getReturnValue() {
        return returnValue;
    }

    public void setReturnValue(Object returnValue) {
        this.returnValue = returnValue;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
