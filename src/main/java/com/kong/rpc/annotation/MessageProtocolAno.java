package com.kong.rpc.annotation;

import java.lang.annotation.*;

/**
 * @author k
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MessageProtocolAno {
    String value() default "";
}
