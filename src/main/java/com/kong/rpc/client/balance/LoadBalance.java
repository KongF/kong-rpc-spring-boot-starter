package com.kong.rpc.client.balance;

import com.kong.rpc.common.service.Service;

import java.util.List;

public interface LoadBalance {
    /**
     *
     * @param services
     * @return
     */
    Service chooseOne(List<Service> services);
}
