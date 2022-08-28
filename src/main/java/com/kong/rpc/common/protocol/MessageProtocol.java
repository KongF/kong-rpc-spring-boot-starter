package com.kong.rpc.common.protocol;

/**
 * 消息协议，定义编组请求，解组请求，编组响应，解组响应规范
 *
 * @author k
 * @since 1.0.0
 */
public interface MessageProtocol {
    /**
     * 编组请求
     * @param req 请求
     * @return 请求byte数组
     * @throws Exception
     */
    byte[] marshallingRequest(RpcRequest req) throws Exception;

    /**
     * 解组请求
     * @param data 请求byte数组
     * @throws Exception
     */
    RpcRequest unmarshallingRequest(byte[] data) throws Exception;

    /**
     * 编组响应
     * @return
     * @throws Exception
     */
    byte[] marshallingResponse(RpcResponse rsp) throws Exception;

    /**
     * 解组响应
     * @param data 响应字节数组
     * @return
     * @throws Exception
     */
    RpcResponse unmarshallingResponse(byte[] data) throws Exception;

}
