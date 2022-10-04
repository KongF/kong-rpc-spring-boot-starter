package com.kong.rpc.common.protocol;

import com.kong.rpc.annotation.MessageProtocolAno;
import com.kong.rpc.common.constants.RpcConstant;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.objenesis.strategy.StdInstantiatorStrategy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Kryo 序列化
 *
 * @author k
 */
@MessageProtocolAno(RpcConstant.PROTOCOL_KRYO)
public class KryoMessageProtocol implements MessageProtocol {

    private static final ThreadLocal<Kryo> kryoLocal = new ThreadLocal<Kryo>(){
        @Override
        protected Kryo initialValue(){
            Kryo kryo = new Kryo();
            kryo.setReferences(false);
            kryo.register(RpcRequest.class);
            kryo.register(RpcResponse.class);
//            kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
            Kryo.DefaultInstantiatorStrategy strategy = (Kryo.DefaultInstantiatorStrategy) kryo.getInstantiatorStrategy();
            strategy.setFallbackInstantiatorStrategy(new StdInstantiatorStrategy());
            return kryo;
        }
    };

    public static Kryo getInstance() {
        return kryoLocal.get();
    }
    @Override
    public byte[] marshallingRequest(RpcRequest req) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        Output output = new Output(bout);
        Kryo kryo = getInstance();
        kryo.writeClassAndObject(output,req);
        byte[] bytes = output.toBytes();
        output.flush();
        return bytes;
    }

    @Override
    public RpcRequest unmarshallingRequest(byte[] data) throws Exception {
        ByteArrayInputStream bin = new ByteArrayInputStream(data);
        Input input = new Input(bin);
        Kryo kryo = getInstance();
        RpcRequest request = (RpcRequest) kryo.readClassAndObject(input);
        input.close();
        return request;
    }

    @Override
    public byte[] marshallingResponse(RpcResponse rsp) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        Output output = new Output(bout);
        Kryo kryo = getInstance();
        kryo.writeClassAndObject(output,rsp);
        byte[] bytes = output.toBytes();
        output.flush();
        return bytes;
    }

    @Override
    public RpcResponse unmarshallingResponse(byte[] data) throws Exception {
        ByteArrayInputStream bin = new ByteArrayInputStream(data);
        Input input = new Input(bin);
        Kryo kryo = getInstance();
        RpcResponse response = (RpcResponse) kryo.readClassAndObject(input);
        input.close();
        return response;
    }
}
