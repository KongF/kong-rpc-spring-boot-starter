package com.kong.rpc.client.net;

import com.kong.rpc.common.protocol.MessageProtocol;
import com.kong.rpc.common.protocol.RpcRequest;
import com.kong.rpc.common.protocol.RpcResponse;
import com.kong.rpc.common.service.Service;
import com.kong.rpc.execption.RpcException;

/**
 * 网络请求客户端，定义网络请求规范
 * @author k
 * @since 1.0.0
 */
public interface NetClient {
    byte[] sendRequest(byte[] data, Service service) throws InterruptedException;

    RpcResponse sendRequest(RpcRequest rpcRequest, Service service, MessageProtocol messageProtocol) throws RpcException;
}
