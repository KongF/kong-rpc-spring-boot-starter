package com.kong.rpc.common.protocol;

import com.kong.rpc.annotation.MessageProtocolAno;
import com.kong.rpc.common.constants.RpcConstant;
import com.kong.rpc.utils.SerializingUtil;

/**
 * Protobuf 序列化协议
 * @author k
 */
@MessageProtocolAno(RpcConstant.PROTOCOL_PROTOBUF)
public class ProtoBufMessageProtocol implements MessageProtocol{
    @Override
    public byte[] marshallingRequest(RpcRequest req) throws Exception {
        return SerializingUtil.serialize(req);
    }

    @Override
    public RpcRequest unmarshallingRequest(byte[] data) throws Exception {
        return SerializingUtil.deserialize(data,RpcRequest.class);
    }

    @Override
    public byte[] marshallingResponse(RpcResponse rsp) throws Exception {
        return SerializingUtil.serialize(rsp);
    }

    @Override
    public RpcResponse unmarshallingResponse(byte[] data) throws Exception {
        return SerializingUtil.deserialize(data,RpcResponse.class);
    }
}
