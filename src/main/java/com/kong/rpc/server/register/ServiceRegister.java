package com.kong.rpc.server.register;

/**
 * 服务注册器，定义服务注册规范
 * @author k
 * @since 1.0.0
 */
public interface ServiceRegister {
    void register(ServiceObject so) throws Exception;
    ServiceObject getServiceObject(String name) throws Exception;
}
