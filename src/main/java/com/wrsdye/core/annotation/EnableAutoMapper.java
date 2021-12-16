package com.wrsdye.core.annotation;

import com.wrsdye.core.handler.MapperLoadHandler;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @author wangrx
 * @description 自动注册mapper开关
 * @date 2021/12/2 下午4:37
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(MapperLoadHandler.class)
@Documented
public @interface EnableAutoMapper {
    String[] basePackages() default {};
}
