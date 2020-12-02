package com.a360inhands.feign.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface FeignApi {
    /**
     * 调用的服务地址
     * @return
     */
    String serviceUrl();
}
