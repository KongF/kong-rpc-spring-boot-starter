package com.kong.rpc.client.discovery;

import com.kong.rpc.common.service.Service;

import java.util.List;

/**
 * 服务发现接口
 * @author k
 */
public interface ServiceDiscoverer {
    List<Service> getServices(String name);
}
