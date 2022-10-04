package com.kong.rpc.server.register;

import com.alibaba.fastjson.JSON;
import com.kong.rpc.common.serializer.ZookeeperSerializer;
import com.kong.rpc.common.service.Service;
import org.I0Itec.zkclient.ZkClient;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLEncoder;

import static com.kong.rpc.common.constants.RpcConstant.*;

/**
 * Zookeeper 服务注册器
 * @author k
 * @since 1.0.0
 */
public class ZookeeperExportServiceRegister extends DefaultServiceRegister implements ServiceRegister {
    private ZkClient zkClient;
    public ZookeeperExportServiceRegister(String zkAddress,Integer port,String protocol,Integer weight){
        zkClient = new ZkClient(zkAddress);
        zkClient.setZkSerializer(new ZookeeperSerializer());
        this.port = port;
        this.protocol = protocol;
        this.weight = weight;
    }
    @Override
    public void register(ServiceObject so) throws Exception {
        super.register(so);
        Service service = new Service();
        String host = InetAddress.getLocalHost().getHostAddress();
        String address = host+":"+port;
        service.setAddress(address);
        service.setName(so.getClazz().getName());
        service.setProtocol(protocol);
        this.exportService(service);
    }
    private void exportService(Service serviceResource){
        String serviceName = serviceResource.getName();
        String uri = JSON.toJSONString(serviceResource);
        try{
            uri = URLEncoder.encode(uri,UTF_8);
        }catch (UnsupportedEncodingException e){
            e.printStackTrace();
        }
        String servicePath = ZK_SERVICE_PATH +PATH_DELIMITER+serviceName+"/service";
        if(!zkClient.exists(servicePath)){
            zkClient.createPersistent(servicePath,true);
        }
        String uriPath = servicePath+PATH_DELIMITER+uri;
        if(zkClient.exists(uriPath)){
            zkClient.delete(uriPath);
        }
        zkClient.createEphemeral(uriPath);
    }
}
