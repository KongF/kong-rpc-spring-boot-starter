package com.kong.rpc.client;

import com.kong.rpc.client.balance.LoadBalance;
import com.kong.rpc.client.cache.ServerDiscoveryCache;
import com.kong.rpc.client.discovery.ServiceDiscoverer;
import com.kong.rpc.client.net.NetClient;
import com.kong.rpc.common.protocol.MessageProtocol;
import com.kong.rpc.common.protocol.RpcRequest;
import com.kong.rpc.common.protocol.RpcResponse;
import com.kong.rpc.common.service.Service;
import com.kong.rpc.execption.RpcException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.*;

import static java.lang.reflect.Proxy.newProxyInstance;

/**
 * 客户端代理工厂，创建远程服务代理
 * @author k
 */
public class ClientProxyFactory {
    private ServiceDiscoverer serviceDiscoverer;


    private Map<String, MessageProtocol> supportMessageProtocols;

    private NetClient netClient;

    private Map<Class<?>,Object> objectCache = new HashMap<>();

    private LoadBalance loadBalance;

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
            List<Service> services = getServiceList(serviceName);

            if(services == null || services.isEmpty()){
                throw new RpcException("No provider available!");
            }
            // 随机选择一个服务提供者(软负载)
            Service service = loadBalance.chooseOne(services);

            //2、构造request对象
            RpcRequest req = new RpcRequest();
            //TODO snowerflower
            req.setRequestId(UUID.randomUUID().toString());
            req.setServiceName(service.getName());
            req.setMethod(method.getName());
            req.setParameterTypes(method.getParameterTypes());
            req.setParameters(args);
            //3、协议层编组
            MessageProtocol protocol = supportMessageProtocols.get(service.getProtocol());
            RpcResponse rsp = netClient.sendRequest(req,service,protocol);
//            byte[] data = protocol.marshallingRequest(req);
//            //4、调用网络层发送请求
//            byte[] repData = netClient.sendRequest(data,service);
//            //5、解组响应信息
//            RpcResponse rsp = protocol.unmarshallingResponse(repData);
            if (rsp == null) {
                throw new RpcException("the response is null");
            }
            //6、结果处理
            if (rsp.getException() != null){
                throw rsp.getException();
            }
            return rsp.getReturnValue();
        }
    }

    private List<Service> getServiceList(String serviceName) throws RpcException {
        List<Service> services;
        synchronized (serviceName) {
            if(ServerDiscoveryCache.isEmpty(serviceName)){
                services = serviceDiscoverer.getServices(serviceName);
                if(services == null || services.size()<=0){
                    throw new RpcException("No provider available!");
                }
                ServerDiscoveryCache.put(serviceName,services);
            }else {
                services = ServerDiscoveryCache.get(serviceName);
            }
        }
        return services;
    }
    public LoadBalance getLoadBalance() {
        return loadBalance;
    }

    public void setLoadBalance(LoadBalance loadBalance) {
        this.loadBalance = loadBalance;
    }
}
