package com.kong.rpc.config;

import com.kong.rpc.client.ClientProxyFactory;
import com.kong.rpc.client.discovery.ZookeeperServiceDiscoverer;
import com.kong.rpc.client.net.NettyNetClient;
import com.kong.rpc.common.protocol.JavaSerializeMessageProtocol;
import com.kong.rpc.common.protocol.MessageProtocol;
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

import java.util.HashMap;
import java.util.Map;

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
    public ClientProxyFactory clientProxyFactory(@Autowired RpcProperty rpcProperty) {
        ClientProxyFactory clientProxyFactory = new ClientProxyFactory();

        //设置服务发现者
        clientProxyFactory.setSid(new ZookeeperServiceDiscoverer(rpcProperty.getRegisterAddress()));

        //设置支持协议
        Map<String, MessageProtocol> supportMessageProtocols = new HashMap<>();
        supportMessageProtocols.put(rpcProperty.getProtocol(),new JavaSerializeMessageProtocol());
        clientProxyFactory.setSupportMessageProtocols(supportMessageProtocols);

        //设置网络层实现
        clientProxyFactory.setNetClient(new NettyNetClient());
        return clientProxyFactory;
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
}