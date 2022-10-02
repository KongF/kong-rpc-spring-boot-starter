package com.kong.rpc.client.cache;

import com.alibaba.fastjson.JSONArray;
import com.kong.rpc.common.service.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 服务发现本地缓存
 * @author k
 */
public class ServerDiscoveryCache {
    /**
     * key:serviceName
     */
    private static final Map<String, List<Service>> SERVER_MAP = new ConcurrentHashMap<>();
   /**
    * 客户端注入的远程服务service class
    */
    public static final List<String> SERVICE_CLASS_NAMES = new ArrayList<>();

    public static void put(String serviceName,List<Service> serviceList){
        SERVER_MAP.put(serviceName,serviceList);
    }

    public static void remove(String serviceName,Service service){
        SERVER_MAP.computeIfPresent(serviceName,(key,value)->value.stream()
        .filter(o->!o.toString().equals(service.toString())).collect(Collectors.toList()));
    }
    public static void removeAll(String serviceName) {
        SERVER_MAP.remove(serviceName);
    }
    public static boolean isEmpty(String serviceName) {
        return SERVER_MAP.get(serviceName) == null || SERVER_MAP.get(serviceName).size() == 0 ;
    }
    public static List<Service> get(String serviceName) {
        return SERVER_MAP.get(serviceName);
    }
}
