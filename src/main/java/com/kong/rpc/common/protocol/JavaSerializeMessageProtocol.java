package com.kong.rpc.common.protocol;

import com.kong.rpc.annotation.MessageProtocolAno;
import com.kong.rpc.common.constants.RpcConstant;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

@MessageProtocolAno(RpcConstant.PROTOCOL_JAVA)
public class JavaSerializeMessageProtocol implements MessageProtocol{

    private byte[] serialize(Object o) throws Exception{
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bout);
        out.writeObject(o);
        return bout.toByteArray();
    }
    @Override
    public byte[] marshallingRequest(RpcRequest req) throws Exception {
        return this.serialize(req);
    }

    @Override
    public RpcRequest unmarshallingRequest(byte[] data) throws Exception {
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data));
        return (RpcRequest) in.readObject();
    }

    @Override
    public byte[] marshallingResponse(RpcResponse rsp) throws Exception {
        return this.serialize(rsp);
    }

    @Override
    public RpcResponse unmarshallingResponse(byte[] data) throws Exception {
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data));
        return (RpcResponse) in.readObject();
    }
}
