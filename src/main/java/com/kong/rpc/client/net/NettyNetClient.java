package com.kong.rpc.client.net;

import com.kong.rpc.client.net.handler.SendHandler;
import com.kong.rpc.common.service.Service;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
