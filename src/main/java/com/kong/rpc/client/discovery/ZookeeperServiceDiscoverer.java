package com.kong.rpc.client.discovery;

import com.alibaba.fastjson.JSON;
import com.kong.rpc.common.constants.RpcConstant;
import com.kong.rpc.common.serializer.ZookeeperSerializer;
import com.kong.rpc.common.service.Service;
import org.I0Itec.zkclient.ZkClient;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author k
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
