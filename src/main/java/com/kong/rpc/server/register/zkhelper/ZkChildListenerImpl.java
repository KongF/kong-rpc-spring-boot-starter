package com.kong.rpc.server.register.zkhelper;

import com.kong.rpc.client.cache.ServerDiscoveryCache;
import org.I0Itec.zkclient.IZkChildListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ZkChildListenerImpl implements IZkChildListener {
    private static Logger logger = LoggerFactory.getLogger(ZkChildListenerImpl.class);
    @Override
    public void handleChildChange(String parentPath, List<String> childList) throws Exception {
        logger.debug("Child change parent path:[{}]\n childList:[{}]",parentPath,childList);
        String[] arr = parentPath.split("/");
        ServerDiscoveryCache.removeAll(arr[2]);
    }
}
