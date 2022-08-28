package com.kong.rpc.annotation;

import java.lang.annotation.*;

/**
 * 注入远程服务
 * @author k
 * @since
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface InjectService {
}
