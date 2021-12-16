package com.wrsdye.core.annotation;

import java.lang.annotation.*;

/**
 * @author wangrx
 * @description mapper构建标识
 * @date 2021/12/2 下午8:34
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BuildMapper {

    String value() default "";
}
