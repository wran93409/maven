package com.wrsdye.core.handler;

import com.wrsdye.core.annotation.BuildMapper;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.Set;

/**
 * @author wangrx
 * @description 自定义注解扫描器
 * @date 2021/12/3 下午1:57
 */
public class MapperClassPathBeanDefinitionScanner extends ClassPathBeanDefinitionScanner {
    public MapperClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry) {
        super(registry);
    }

    @Override
    public Set<BeanDefinitionHolder> doScan(String... basePackages){
        this.setBeanNameGenerator(new AnnotationBeanNameGenerator());
        this.addIncludeFilter(new AnnotationTypeFilter(BuildMapper.class));
        return super.doScan(basePackages);
    }
}
