package com.kong.rpc.config;

import com.kong.rpc.annotation.LoadBalanceAno;
import com.kong.rpc.annotation.MessageProtocolAno;
import com.kong.rpc.client.ClientProxyFactory;
import com.kong.rpc.client.balance.LoadBalance;
import com.kong.rpc.client.discovery.ZookeeperServiceDiscoverer;
import com.kong.rpc.client.net.NettyNetClient;
import com.kong.rpc.common.protocol.JavaSerializeMessageProtocol;
import com.kong.rpc.common.protocol.MessageProtocol;
import com.kong.rpc.execption.RpcException;
import com.kong.rpc.properties.RpcProperty;
import com.kong.rpc.server.NettyRpcServer;
import com.kong.rpc.server.RequestHandler;
import com.kong.rpc.server.RpcServer;
import com.kong.rpc.server.register.DefaultRpcProcessor;
import com.kong.rpc.server.register.ServiceRegister;
import com.kong.rpc.server.register.ZookeeperExportServiceRegister;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * boot 自动配置
 * @author k
 */
@Configuration
public class AutoConfiguration {
    @Bean
    public DefaultRpcProcessor defaultRpcProcessor() {
        return new DefaultRpcProcessor();
    }
    @Bean
    public ClientProxyFactory clientProxyFactory(@Autowired RpcProperty rpcProperty) throws RpcException {
        ClientProxyFactory clientProxyFactory = new ClientProxyFactory();

        //设置服务发现者
        clientProxyFactory.setSid(new ZookeeperServiceDiscoverer(rpcProperty.getRegisterAddress()));

        //设置支持协议
        Map<String, MessageProtocol> supportMessageProtocols = buildSupportMessageProtoclos();
        clientProxyFactory.setSupportMessageProtocols(supportMessageProtocols);
        //设置负载均衡算法
        LoadBalance loadBalance = getLoadBalance(rpcProperty.getLoadBalance());
        clientProxyFactory.setLoadBalance(loadBalance);
        //设置网络层实现
        clientProxyFactory.setNetClient(new NettyNetClient());
        return clientProxyFactory;
    }

    private Map<String, MessageProtocol> buildSupportMessageProtoclos() {
        Map<String,MessageProtocol> supportMessageProtocols = new HashMap<>();
        ServiceLoader<MessageProtocol> loader = ServiceLoader.load(MessageProtocol.class);
        Iterator<MessageProtocol> iterator = loader.iterator();
        while (iterator.hasNext()) {
            MessageProtocol messageProtocol = iterator.next();
            MessageProtocolAno ano = messageProtocol.getClass().getAnnotation(MessageProtocolAno.class);
            Assert.notNull(ano,"message protocol name can not be empty!");
            supportMessageProtocols.put(ano.value(),messageProtocol);
        }
        return supportMessageProtocols;
    }

    @Bean
    public ServiceRegister serviceRegister(@Autowired RpcProperty rpcProperty) {
        return new ZookeeperExportServiceRegister(rpcProperty.getRegisterAddress(),rpcProperty.getServerPort(),rpcProperty.getProtocol());
    }
    @Bean
    public RequestHandler requestHandler(@Autowired ServiceRegister serviceRegister) {
        return new RequestHandler(new JavaSerializeMessageProtocol(), serviceRegister);
    }
    @Bean
    public RpcServer rpcServer(@Autowired RequestHandler requestHandler, @Autowired RpcProperty rpcProperty){
        return new NettyRpcServer(rpcProperty.getServerPort(),rpcProperty.getProtocol(),requestHandler);
    }
    @Bean
    public RpcProperty rpcProperty() {
        return new RpcProperty();
    }

    private LoadBalance getLoadBalance(String name) throws RpcException {
        ServiceLoader<LoadBalance> loader = ServiceLoader.load(LoadBalance.class);
        Iterator<LoadBalance> iterator = loader.iterator();
        while (iterator.hasNext()){
            LoadBalance loadBalance = iterator.next();
            LoadBalanceAno ano = loadBalance.getClass().getAnnotation(LoadBalanceAno.class);
            Assert.notNull(ano,"load balance name can not be empty!");
            if(name.equals(ano.value())){
                return loadBalance;
            }
        }
        throw new RpcException("invalid load balance config");
    }
}
