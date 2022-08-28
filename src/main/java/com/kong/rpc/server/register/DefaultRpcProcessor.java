package com.kong.rpc.server.register;

import com.kong.rpc.annotation.InjectService;
import com.kong.rpc.annotation.Service;
import com.kong.rpc.client.ClientProxyFactory;
import com.kong.rpc.server.RpcServer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;

/**
 * RPC处理者，支持服务启动暴露，自动注入Service
 *
 * @author k
 * @since 1.0.0
 */
public class DefaultRpcProcessor implements ApplicationListener<ContextRefreshedEvent> {

    @Resource
    private ClientProxyFactory clientProxyFactory;

    @Resource
    private ServiceRegister serviceRegister;

    @Resource
    private RpcServer rpcServer;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (Objects.isNull(event.getApplicationContext().getParent())){
            ApplicationContext context = event.getApplicationContext();
            startServer(context);

            injectService(context);
        }
    }

    private void injectService(ApplicationContext context) {
        String[] names = context.getBeanDefinitionNames();
        for(String name : names){
            Class<?> clazz = context.getType(name);
            if (Objects.isNull(clazz)) continue;
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                InjectService injectService = field.getAnnotation(InjectService.class);
                if (Objects.isNull(injectService)) continue;
                Class<?> fieldClass = field.getType();
                Object object = context.getBean(name);
                field.setAccessible(true);
                try{
                    field.set(object,clientProxyFactory.getProxy(fieldClass));
                }catch (IllegalAccessException e){
                    e.printStackTrace();
                }
            }
        }
    }

    private void startServer(ApplicationContext context) {
        Map<String,Object> beans = context.getBeansWithAnnotation(Service.class);
        if(beans.size() != 0) {
            boolean startServerFlag = true;
            for(Object obj : beans.values()) {
                try{
                    Class<?> clazz = obj.getClass();
                    Class<?>[] interfaces = clazz.getInterfaces();
                    ServiceObject so;
                    if (interfaces.length != 1) {
                        Service service = clazz.getAnnotation(Service.class);
                        String value = service.value();
                        if(value.equals("")){
                            startServerFlag = false;
                            throw new UnsupportedOperationException("The exposed interface is not specific with '"+obj.getClass().getName()+"'");
                        }
                        so = new ServiceObject(value,Class.forName(value),obj);
                    }else {
                        Class<?> supperClass = interfaces[0];
                        so = new ServiceObject(supperClass.getName(),supperClass,obj);
                    }
                    serviceRegister.register(so);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            if(startServerFlag) {
                rpcServer.start();
            }
        }
    }
}
