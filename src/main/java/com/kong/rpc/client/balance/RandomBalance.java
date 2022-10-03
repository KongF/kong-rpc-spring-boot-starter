package com.kong.rpc.client.balance;

import com.kong.rpc.annotation.LoadBalanceAno;
import com.kong.rpc.common.constants.RpcConstant;
import com.kong.rpc.common.service.Service;

import java.util.List;
import java.util.Random;

/**
 * 随机
 * @author k
 */
@LoadBalanceAno(RpcConstant.BALANCE_RANDOM)
public class RandomBalance implements LoadBalance{
    private static Random random = new Random();
    @Override
    public synchronized Service chooseOne(List<Service> services) {
        return services.get(random.nextInt(services.size()));
    }
}
