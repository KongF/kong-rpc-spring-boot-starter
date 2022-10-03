package com.kong.rpc.client.balance;

import com.kong.rpc.annotation.LoadBalanceAno;
import com.kong.rpc.common.constants.RpcConstant;
import com.kong.rpc.common.service.Service;

import java.util.List;

/**
 * @author k
 */
@LoadBalanceAno(RpcConstant.BALANCE_WEIGHT_ROUND)
public class WeightRoundBalance implements LoadBalance{
    private static int index;
    @Override
    public synchronized Service chooseOne(List<Service> services) {
        int allWeight = services.stream().mapToInt(Service::getWeight).sum();
        int number = (index++) % allWeight;
        for (Service service : services) {
            if (service.getWeight()>number){
                return service;
            }
            number -= service.getWeight();
        }
        return null;
    }
}
