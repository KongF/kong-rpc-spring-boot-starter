package com.kong.rpc.common.serializer;

import org.I0Itec.zkclient.exception.ZkMarshallingError;
import org.I0Itec.zkclient.serialize.ZkSerializer;

import java.nio.charset.StandardCharsets;

/**
 * Zookeeper序列号
 * @author k
 * @since 1.0.0
 */
public class ZookeeperSerializer implements ZkSerializer {

    /**
     * 序列化
     * @param o
     * @return
     * @throws ZkMarshallingError
     */
    @Override
    public byte[] serialize(Object o) {
        return String.valueOf(o).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 反序列化
     * @param bytes
     * @return
     * @throws ZkMarshallingError
     */
    @Override
    public Object deserialize(byte[] bytes) {
        return new String(bytes,StandardCharsets.UTF_8);
    }
}
