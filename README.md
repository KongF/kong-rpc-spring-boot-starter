# 手写RPC框架

## 一、内容概览

- RPC的概念及运作流程

- RPC协议及RPC框架的概念

- Netty的基本使用

- Java序列化及反序列化

- Zookeeper注册中心的基本使用

- 自定义注解实现特殊业务逻辑

- Java的动态代理

- 自定义Spring Boot Starter

## 二、RPC基础知识

### 2.1 RPC

Remote Procedure Call（RPC）：远程过程调用。

借助网络通信实现想通用本地方法一样调用远程方法。

### 2.2 RPC的流程

> 1、客户端处理过程中调用Client stub（就像调用本地方法一样），传递参数；
> 2、Client stub将参数编组为消息，然后通过系统调用向服务端发送消息；
> 3、客户端本地操作系统将消息从客户端机器发送到服务端机器；
> 4、服务端操作系统将接收到的数据包传递给Server stub;
> 5、Server stub解组消息为参数；
> 6、Server stub再调用服务端的过程，过程执行结果以反方向的相同步骤响应给客户端。

![请添加图片描述](https://img-blog.csdnimg.cn/b6cca68b5e2c4701bd15c5f703fc04fc.png)

### 2.3 RPC要解决两个问题

1、分布式系统中服务间调用问题。

2、远程调用是，要能够像本地方法一样方便，让调用者感知不到远程调用的逻辑。

### 2.4 RPC协议是什么？

RPC调用过程中需要将参数编组为消息进行发送，接受方需要解组消息为参数，过程处理结果同样需要经编组、解组。消息由哪些部分构成及消息的表示形式就构成了消息协议。

RPC调用过程中采用的消息协议称为RPC协议

- RPC协议规定请求、响应消息的格式

- 在TCP（网络传输控制协议）上可选用或自定义消息协议来完成RPC消息交互

我们可以选用通用的标准协议（如：http、https），也也可根据自身的需要定义自己的消息协议。

### 2.5 RPC框架：

传统的webservice框架：Apache CXF、Apache Axis2、Java自带的JAX-WS等。webservice框架大多基于标准的SOAP协议。
新兴的微服务框架：Dubbo、spring cloud、Apache Thrift等。

## 三、手写RPC

### 3.1 目标

本文会实现一个简单的RPC框架，zookeeper作为注册中心，具备服务注册与暴露，服务发现能力，项目名：kong-rpc-spring-boot-starter,

### 3.2 项目整体结构

![请添加图片描述](https://img-blog.csdnimg.cn/57127da52898419a90e06e625d04c99d.png)

client：客户端实现服务发现，服务代理调用远程服务

server：服务端要提供远程服务，具备**服务注册及暴露**的能力；在这之后，还需要开启**网络服务**，供客户端连接。

### 3.3 客户端实现

1、服务发现者

> ServiceDIscoverer .java

```java
/**
 * 服务发现接口
 * @author k
 */
public interface ServiceDiscoverer {
    List<Service> getServices(String name);
}
```

> ZookeeperServiceDiscoverer.java 服务发现者

```java
/**
 * @author k
 * @since 1.0.0
 */
public class ZookeeperServiceDiscoverer implements ServiceDiscoverer {
    private ZkClient zkClient;

    public ZookeeperServiceDiscoverer(String zkAddress){
        zkClient = new ZkClient(zkAddress);
        zkClient.setZkSerializer(new ZookeeperSerializer());
    }
    @Override
    public List<Service> getServices(String name) {
        String servicePath = RpcConstant.ZK_SERVICE_PATH+RpcConstant.PATH_DELIMITER+name+"/service";
        List<String> childrenList = zkClient.getChildren(servicePath);
        return Optional.ofNullable(childrenList).orElse(new ArrayList<>()).stream().map(str -> {
            String deCh = null;
            try{
                deCh = URLDecoder.decode(str,RpcConstant.UTF_8);
            }catch (UnsupportedEncodingException e){
                e.printStackTrace();
            }
            return JSON.parseObject(deCh,Service.class);
        }).collect(Collectors.toList());
    }
}
```

2、网络客户端

> NetClient.java

```java
/**
 * 网络请求客户端，定义网络请求规范
 * @author k
 * @since 1.0.0
 */
public interface NetClient {
    byte[] sendRequest(byte[] data, Service service) throws InterruptedException;
}
```

> NettyNetClient.java

```java
/**
 * 定义Netty网络请求的细则
 * @author k
 * @since 1.0.0
 */
public class NettyNetClient implements NetClient {
    private static Logger logger = LoggerFactory.getLogger(NettyNetClient.class);

    /**
     * 发送请求
     *
     * @param data 请求主数据
     * @param service 服务信息
     * @return 响应数据
     * @throws InterruptedException 抛出异常
     */
    @Override
    public byte[] sendRequest(byte[] data, Service service) throws InterruptedException {
        String[] addInfoArray = service.getAddress().split(":");
        String serverAddress = addInfoArray[0];
        String serverPort = addInfoArray[1];

        SendHandler sendHandler = new SendHandler(data);
        byte[] respData;
        //配置客户端
        EventLoopGroup group = new NioEventLoopGroup();
        try{
            Bootstrap b = new Bootstrap();
            b.group(group).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY,true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel channel){
                            ChannelPipeline p = channel.pipeline();
                            p.addLast(sendHandler);
                        }
                    });
            //启动客户端连接
            b.connect(serverAddress,Integer.parseInt(serverPort)).sync();
            respData=(byte[]) sendHandler.rspData();
            logger.info("SendRequest get reply: {}",respData);
        }finally {
            //释放线程组资源
            group.shutdownGracefully();
        }
        return respData;
    }
}
```

> SendHanler.java

```java
package com.kong.rpc.client.net.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

/**
 * 发现处理类，定义Netty入站处理规则
 * @author k
 */
public class SendHandler extends ChannelInboundHandlerAdapter {
    private static Logger logger = LoggerFactory.getLogger(SendHandler.class);

    private CountDownLatch cdl;
    private Object readMsg = null;
    private byte[] data;

    public SendHandler(byte[] data){
        cdl = new CountDownLatch(1);
        this.data = data;
    }

    /**
     * 当连接服务端成功后，发送请求数据
     * @param ctx 通道上下文
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        logger.info("Successful connection to server : {}",ctx);
        ByteBuf reqBuf = Unpooled.buffer(data.length);
        reqBuf.writeBytes(data);
        logger.info("Client sends message: {}",reqBuf);
        ctx.writeAndFlush(reqBuf);
    }

    /**
     * 读取数据，读取完毕后释放CD锁
     * @param ctx 通道上下文
     * @param msg ByteBuf
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx,Object msg){
        logger.info("Client reads message: {}",msg);
        ByteBuf msgBuf = (ByteBuf) msg;
        byte[] resp = new byte[msgBuf.readableBytes()];
        msgBuf.readBytes(resp);
        readMsg = resp;
        cdl.countDown();
    }

    /**
     * 等待读取完成
     * @return
     * @throws InterruptedException
     */
    public Object rspData() throws InterruptedException {
        cdl.await();
        return readMsg;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx){
        ctx.flush();
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx,Throwable cause){
        cause.printStackTrace();
        logger.error("Exception occurred : {}",cause.getMessage());
        ctx.close();
    }
}
```

3、服务代理

**服务代理**来执行服务调用

> ClientProxyFactory.java

```java
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
```

4、消息协议

java 序列化消息协议

> MessageProtocol

### 3.4 服务端实现

服务端提供远程服务

1、服务注册

> ServiceRegister
> 
> 将指定ServiceObject对象序列化后保存到ZooKeeper上，供客户端发现。同时会将服务对象缓存起来，在客户端调用服务时，通过缓存的ServiceObject对象反射指定服务，调用方法。

```java
public interface ServiceRegister {
    void register(ServiceObject so) throws Exception;
    ServiceObject getServiceObject(String name) throws Exception;
}
```

```java
/**
 * Zookeeper 服务注册器
 * @author k
 * @since 1.0.0
 */
public class ZookeeperExportServiceRegister extends DefaultServiceRegister implements ServiceRegister {
    private ZkClient zkClient;
    public ZookeeperExportServiceRegister(String zkAddress,Integer port,String protocol){
        zkClient = new ZkClient(zkAddress);
        zkClient.setZkSerializer(new ZookeeperSerializer());
        this.port = port;
        this.protocol = protocol;
    }
    @Override
    public void register(ServiceObject so) throws Exception {
        super.register(so);
        Service service = new Service();
        String host = InetAddress.getLocalHost().getHostAddress();
        String address = host+":"+port;
        service.setAddress(address);
        service.setName(so.getClazz().getName());
        service.setProtocol(protocol);
        this.exportService(service);
    }
    private void exportService(Service serviceResource){
        String serviceName = serviceResource.getName();
        String uri = JSON.toJSONString(serviceResource);
        try{
            uri = URLEncoder.encode(uri,UTF_8);
        }catch (UnsupportedEncodingException e){
            e.printStackTrace();
        }
        String servicePath = ZK_SERVICE_PATH +PATH_DELIMITER+serviceName+"/service";
        if(!zkClient.exists(servicePath)){
            zkClient.createPersistent(servicePath,true);
        }
        String uriPath = servicePath+PATH_DELIMITER+uri;
        if(zkClient.exists(uriPath)){
            zkClient.delete(uriPath);
        }
        zkClient.createEphemeral(uriPath);
    }
}
```

2、网络服务

> NettyRpcServer 主要提供Netty网络服务开启和关闭能力

```java
/**
 * Netty RPC 服务端，提供Nett网络服务开启、关闭
 * @author k
 * @since 1.0.0
 */
public class NettyRpcServer extends RpcServer {
    private static Logger logger = LoggerFactory.getLogger(NettyRpcServer.class);
    private Channel channel;

    public NettyRpcServer(int port, String protocol, RequestHandler handler) {
        super(port, protocol, handler);
    }

    @Override
    public void start() {
        // 配置服务器
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try{
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup,workerGroup).channel(NioServerSocketChannel.class).option(ChannelOption.SO_BACKLOG,100)
                    .handler(new LoggingHandler(LogLevel.INFO)).childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                public void initChannel(SocketChannel ch){
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(new ChannelRequestHandler());
                    }
                    });
            // 启动服务
            ChannelFuture f = b.bind(port).sync();
            logger.info("Server started successfully.");
            channel = f.channel();
            f.channel().closeFuture().sync();
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    @Override
    public void stop() {
        this.channel.close();
    }
    private class ChannelRequestHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            logger.info("Channel active: {}",ctx);
        }
        @Override
        public void channelRead(ChannelHandlerContext ctx,Object msg) throws Exception {
            logger.info("The server receives a message : {}",msg);
            ByteBuf msgBuf = (ByteBuf) msg;
            byte[] req = new byte[msgBuf.readableBytes()];
            msgBuf.readBytes(req);
            byte[] res = handler.handleRequest(req);
            logger.info("Send response: {}",msg);
            ByteBuf respBuf = Unpooled.buffer(res.length);
            respBuf.writeBytes(res);
            ctx.write(respBuf);
        }
        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) {
            ctx.flush();
        }
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            // Close the connection when an exception is raised.
            cause.printStackTrace();
            logger.error("Exception occurred：{}", cause.getMessage());
            ctx.close();
        }
    }
}
```

3、RPC处理

> 这里引入一个**RPC处理者**的概念，负责开启服务，以及注入服务。

```java
/**
 * RPC处理者，支持服务启动暴露，自动注入Service
 *
 * @author k
 * @since 1.0.0
 */
public class DefaultRpcProcessor implements ApplicationListener<ContextRefreshedEvent> {

    @Resource
    private ClientProxyFactory clientProxyFactory;

    @Resource
    private ServiceRegister serviceRegister;

    @Resource
    private RpcServer rpcServer;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (Objects.isNull(event.getApplicationContext().getParent())){
            ApplicationContext context = event.getApplicationContext();
            startServer(context);

            injectService(context);
        }
    }

    private void injectService(ApplicationContext context) {
        String[] names = context.getBeanDefinitionNames();
        for(String name : names){
            Class<?> clazz = context.getType(name);
            if (Objects.isNull(clazz)) continue;
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                InjectService injectService = field.getAnnotation(InjectService.class);
                if (Objects.isNull(injectService)) continue;
                Class<?> fieldClass = field.getType();
                Object object = context.getBean(name);
                field.setAccessible(true);
                try{
                    field.set(object,clientProxyFactory.getProxy(fieldClass));
                }catch (IllegalAccessException e){
                    e.printStackTrace();
                }
            }
        }
    }

    private void startServer(ApplicationContext context) {
        Map<String,Object> beans = context.getBeansWithAnnotation(Service.class);
        if(beans.size() != 0) {
            boolean startServerFlag = true;
            for(Object obj : beans.values()) {
                try{
                    Class<?> clazz = obj.getClass();
                    Class<?>[] interfaces = clazz.getInterfaces();
                    ServiceObject so;
                    if (interfaces.length != 1) {
                        Service service = clazz.getAnnotation(Service.class);
                        String value = service.value();
                        if(value.equals("")){
                            startServerFlag = false;
                            throw new UnsupportedOperationException("The exposed interface is not specific with '"+obj.getClass().getName()+"'");
                        }
                        so = new ServiceObject(value,Class.forName(value),obj);
                    }else {
                        Class<?> supperClass = interfaces[0];
                        so = new ServiceObject(supperClass.getName(),supperClass,obj);
                    }
                    serviceRegister.register(so);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            if(startServerFlag) {
                rpcServer.start();
            }
        }
    }
}
```

### 3.5 注解

定义Service和InjectService两个注解实现服务注册和注入依赖

> - @Service :服务注册
> 
> - @InjectService：注入服务

## 四、使用

### 4.1 生成本地Maven依赖

```
# 进入源码pom文件目录，执行命令
mvn clean install
```

### 4.2 服务提供者、消费者同时引入Maven依赖

```
<dependency>
    <groupId>com.kong.rpc</groupId>
    <artifactId>kong-rpc-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 4.3 服务提供者、消费者同时配置注册中心

```
kong.rpc.register-address=192.168.2.191:2181
```

### 4.4 服务提供者注解服务示例(@Service)

```java
import com.kong.rpc.annotation.Service;

@Service
public class UserServiceImpl implements UserService {    
    @Override
    public ApiResult<User> getUser(Long id) {        
        User user = getFromDbOrCache(id);        
        return ApiResult.success(user);   
    }    
    private User getFromDbOrCache(Long id) {        
        return new User(id, "user1", 1, "aaa");    
    }
}
```

### 4.5 服务消费者注入服务示例(@InjectService)

```java
@RestController
@RequestMapping("/index/")
public class IndexController {    
    @InjectService
    private UserService userService;    
    /**
     * 获取用户信息
     *
     * @param id 用户id
     * @return 用户信息
     */
    @GetMapping("getUser")    
    public ApiResult<User> getUser(Long id) {        
    return userService.getUser(id);    
        }
}
```

>  使用 demo示例： https://github.com/KongF/kong-rpc-example.git
