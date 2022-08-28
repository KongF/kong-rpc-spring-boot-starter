package com.kong.rpc.client;

import com.kong.rpc.client.discovery.ServiceDiscoverer;
import com.kong.rpc.client.net.NetClient;
import com.kong.rpc.common.protocol.MessageProtocol;
import com.kong.rpc.common.protocol.RpcRequest;
import com.kong.rpc.common.protocol.RpcResponse;
import com.kong.rpc.common.service.Service;
import com.kong.rpc.execption.RpcException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static java.lang.reflect.Proxy.newProxyInstance;

public class ClientProxyFactory {
    private ServiceDiscoverer serviceDiscoverer;

    private Map<String, MessageProtocol> supportMessageProtocols;

    private NetClient netClient;

    private Map<Class<?>,Object> objectCache = new HashMap<>();

    /**
     * 通过java动态代理获取服务代理类
     * @param clazz 被代理Class
     * @param <T> 泛型
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T getProxy(Class<T> clazz) {
        return (T)this.objectCache.computeIfAbsent(clazz,
                cls -> newProxyInstance(cls.getClassLoader(),new Class<?>[]{cls},new ClientInvocationHandler(cls))
        );
    }
    public ServiceDiscoverer getServiceDiscoverer(){
        return serviceDiscoverer;
    }
    public void setSid(ServiceDiscoverer serviceDiscoverer) {
        this.serviceDiscoverer = serviceDiscoverer;
    }
    public Map<String, MessageProtocol> getSupportMessageProtocols() {
        return supportMessageProtocols;
    }

    public void setSupportMessageProtocols(Map<String, MessageProtocol> supportMessageProtocols) {
        this.supportMessageProtocols = supportMessageProtocols;
    }

    public NetClient getNetClient() {
        return netClient;
    }

    public void setNetClient(NetClient netClient) {
        this.netClient = netClient;
    }
    //客户端服务代理类invoke函数细节实现
    private class ClientInvocationHandler implements InvocationHandler {
        private Class<?> clazz;

        private Random random = new Random();
        public ClientInvocationHandler(Class<?> clazz) {
            super();
            this.clazz = clazz;
        }
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if(method.getName().equals("toString")){
                return proxy.getClass().toString();
            }
            if(method.getName().equals("hashCode")) {
                return 0;
            }

            //1、获得服务信息
            String serviceName = this.clazz.getName();
            List<Service> services = serviceDiscoverer.getServices(serviceName);

            if(services == null || services.isEmpty()){
                throw new RpcException("No provider available!");
            }
            // 随机选择一个服务提供者(软负载)
            Service service = services.get(random.nextInt(services.size()));

            //2、构造request对象
            RpcRequest req = new RpcRequest();
            req.setServiceName(service.getName());
            req.setMethod(method.getName());
            req.setParameterTypes(method.getParameterTypes());
            req.setParameters(args);
            //3、协议层编组
            MessageProtocol protocol = supportMessageProtocols.get(service.getProtocol());
            byte[] data = protocol.marshallingRequest(req);
//            4、调用网络层发送请求
            byte[] repData = netClient.sendRequest(data,service);
            //5、解组响应信息
            RpcResponse rsp = protocol.unmarshallingResponse(repData);

            //6、结果处理
            if (rsp.getException() != null){
                throw rsp.getException();
            }
            return rsp.getReturnValue();
        }
    }
}
