package com.kong.rpc.client.net;

import com.kong.rpc.common.service.Service;

/**
 * 网络请求客户端，定义网络请求规范
 * @author k
 * @since 1.0.0
 */
public interface NetClient {
    byte[] sendRequest(byte[] data, Service service) throws InterruptedException;
}
