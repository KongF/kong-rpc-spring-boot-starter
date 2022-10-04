package com.kong.rpc.server;

import com.kong.rpc.common.constants.RpcConstant;
import com.kong.rpc.common.protocol.MessageProtocol;
import com.kong.rpc.common.protocol.RpcRequest;
import com.kong.rpc.common.protocol.RpcResponse;
import com.kong.rpc.common.protocol.RpcStatus;
import com.kong.rpc.server.register.ServiceObject;
import com.kong.rpc.server.register.ServiceRegister;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 请求处理者，提供解组请求，编组响应
 *
 * @author k
 * @since 1.0.0
 */
public class RequestHandler {
    private MessageProtocol protocol;

    private ServiceRegister serviceRegister;

    public RequestHandler(MessageProtocol protocol,ServiceRegister serviceRegister){
        super();
        this.protocol = protocol;
        this.serviceRegister = serviceRegister;
    }

    public byte[] handleRequest(byte[] data) throws Exception {
        // 1、解组信息
        RpcRequest req = this.protocol.unmarshallingRequest(data);
        // 2、查找服务对象
        ServiceObject so = this.serviceRegister.getServiceObject(req.getServiceName());

        RpcResponse rsp = null;

        if(so == null) {
            rsp = new RpcResponse(RpcStatus.NOT_FOUND);
        }else{
            //3、反射调用对应的过程方法
            try{
                Method m = so.getClazz().getMethod(req.getMethod(),req.getParameterTypes());
                Object returnValue = m.invoke(so.getObj(),req.getParameters());
                rsp = new RpcResponse(RpcStatus.SUCCESS);
                rsp.setReturnValue(returnValue);
            }catch (NoSuchMethodException|SecurityException|IllegalAccessException|IllegalArgumentException| InvocationTargetException exception){
                rsp = new RpcResponse(RpcStatus.ERROR);
                rsp.setException(exception);
            }
        }
        rsp.setRequestId(req.getRequestId());
        return this.protocol.marshallingResponse(rsp);
    }

    public MessageProtocol getProtocol() {
        return protocol;
    }

    public void setProtocol(MessageProtocol protocol) {
        this.protocol = protocol;
    }

    public ServiceRegister getServiceRegister() {
        return serviceRegister;
    }

    public void setServiceRegister(ServiceRegister serviceRegister) {
        this.serviceRegister = serviceRegister;
    }
}
