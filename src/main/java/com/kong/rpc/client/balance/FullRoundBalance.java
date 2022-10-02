package com.kong.rpc.client.balance;

import com.kong.rpc.annotation.LoadBalanceAno;
import com.kong.rpc.common.constants.RpcConstant;
import com.kong.rpc.common.service.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 轮询算法
 * @author k
 */
@LoadBalanceAno(RpcConstant.BALANCE_ROUND)
public class FullRoundBalance implements LoadBalance{
    private static Logger logger = LoggerFactory.getLogger(FullRoundBalance.class);

    private volatile int index;

    @Override
    public synchronized Service chooseOne(List<Service> services) {
        if(index == services.size()){
            index = 0;
        }
        return services.get(index++);
    }
}
