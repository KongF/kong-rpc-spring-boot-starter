package com.kong.rpc.client.net;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.kong.rpc.client.net.handler.SendHandler;
import com.kong.rpc.client.net.handler.SendHandlerV2;
import com.kong.rpc.common.protocol.MessageProtocol;
import com.kong.rpc.common.protocol.RpcRequest;
import com.kong.rpc.common.protocol.RpcResponse;
import com.kong.rpc.common.service.Service;
import com.kong.rpc.execption.RpcException;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.*;

/**
 * 定义Netty网络请求的细则
 * @author k
 * @since 1.0.0
 */
public class NettyNetClient implements NetClient {
    private static Logger logger = LoggerFactory.getLogger(NettyNetClient.class);

    private static ExecutorService threadPool = new ThreadPoolExecutor(4,10,200, TimeUnit.SECONDS,new LinkedBlockingDeque<>(1000),new ThreadFactoryBuilder().setNameFormat("rpcClient-%d").build());

    private EventLoopGroup loopGroup = new NioEventLoopGroup(4);

    public static Map<String,SendHandlerV2> connectedServerNodes = new ConcurrentHashMap<>();
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
/**
 * 每次请求都会调用sendRequest()方法
 */
    @Override
    public RpcResponse sendRequest(RpcRequest rpcRequest, Service service, MessageProtocol messageProtocol) throws RpcException {
        String address = service.getAddress();
        synchronized (address) {
            if (connectedServerNodes.containsKey(address)){
                SendHandlerV2 handlerV2 = connectedServerNodes.get(address);
                logger.info("使用现有连接");
                return handlerV2.sendRequest(rpcRequest);
            }
            String[] addrInfo = address.split(":");
            final String serverAddress = addrInfo[0];
            final String serverPort = addrInfo[1];
            final SendHandlerV2 handlerV2 = new SendHandlerV2(messageProtocol,address);
            threadPool.submit(()->{
                Bootstrap b = new Bootstrap();
                b.group(loopGroup).channel(NioSocketChannel.class)
                        .option(ChannelOption.TCP_NODELAY,true)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel channel) throws Exception {
                                ChannelPipeline pipeline = channel.pipeline();
                                pipeline.addLast(handlerV2);
                            }
                        });
                ChannelFuture channelFuture = b.connect(serverAddress,Integer.parseInt(serverPort));
                channelFuture.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        connectedServerNodes.put(address,handlerV2);
                    }
                });
            });
            logger.info("使用新的连接。。。。");
            return handlerV2.sendRequest(rpcRequest);
        }
    }
}
