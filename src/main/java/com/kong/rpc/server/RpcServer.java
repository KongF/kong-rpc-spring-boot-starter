package com.kong.rpc.server;

/**
 * RPC 服务端抽象类
 *
 * @author k
 * @since
 */
public abstract class RpcServer {
    /**
     * 服务端口
     */
    protected int port;
    /**
     * 服务协议
     */
    protected String protocol;

    /**
     *  请求处理者
     */
    protected RequestHandler handler;

    public RpcServer(int port, String protocol, RequestHandler handler) {
        super();
        this.port = port;
        this.protocol = protocol;
        this.handler = handler;
    }
    public abstract void start();

    public abstract void stop();

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public RequestHandler getHandler() {
        return handler;
    }

    public void setHandler(RequestHandler handler) {
        this.handler = handler;
    }
}
