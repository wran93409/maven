package com.wrsdye.core.handler;

import com.wrsdye.core.CommonBaseMapper;
import com.wrsdye.core.annotation.EnableAutoMapper;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.SignatureAttribute;
import javassist.bytecode.annotation.Annotation;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;

import java.util.Set;

/**
 * @author wangrx
 * @description mapper自动加载处理器
 * @date 2021/12/1 上午11:03
 */
@Slf4j
public class MapperLoadHandler implements BeanDefinitionRegistryPostProcessor, ImportBeanDefinitionRegistrar {

    private final static String MAPPER_NAME = "CustomMapper";

    private final static String PACKAGE_NAME = ".mapper.";

    private String[] basePackages;


    /**
     * 注册当前bean
     * @param metadata
     * @param registry
     */
    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(this.getClass());
        builder.addPropertyValue("basePackages",getPackagesToScan(metadata));
        registry.registerBeanDefinition(this.getClass().getSimpleName(), builder.getBeanDefinition());
    }

    /**
     * 注册mapper到spring容器
     * @param registry
     */
    @SneakyThrows
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry){
        log.info("MapperLoadHandler loading...");
        long time = System.currentTimeMillis();
        int mapperNum = 0;
        MapperClassPathBeanDefinitionScanner scanner = new MapperClassPathBeanDefinitionScanner(registry);
        try {
            Set<BeanDefinitionHolder> beanDefinitionHolders = scanner.doScan(basePackages);
            for(BeanDefinitionHolder beanDefinitionHolder: beanDefinitionHolders){
                String beanClassName = beanDefinitionHolder.getBeanDefinition().getBeanClassName();
                if(StringUtils.isBlank(beanClassName)){
                    continue;
                }
                Class<?> clazz = buildClazz(getBackPackage(beanClassName),StringUtils.capitalize(beanDefinitionHolder.getBeanName()));
                BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(clazz);
                GenericBeanDefinition definition = (GenericBeanDefinition) builder.getRawBeanDefinition();
                definition.getConstructorArgumentValues().addGenericArgumentValue(clazz);
                definition.setBeanClass(MapperFactoryBean.class);
                definition.setAutowireMode(GenericBeanDefinition.AUTOWIRE_BY_TYPE);
                registry.registerBeanDefinition(clazz.getSimpleName(), definition);
                mapperNum++;
            }
        }catch (NotFoundException ne){
            log.error("MapperLoadHandler Loading error, not found bean. Throwable：",ne);
            throw new NotFoundException("MapperLoadHandler Loading error, not found bean. ",ne);
        }catch (CannotCompileException ce){
            log.error("MapperLoadHandler Loading error, cannot compile bean. Throwable：",ce);
            throw new CannotCompileException("MapperLoadHandler Loading error, cannot compile bean.  ",ce);
        }catch (Exception ex){
            log.error("MapperLoadHandler Loading error. Throwable：",ex);
            throw new RuntimeException("MapperLoadHandler Loading error. ",ex);

        }
        long nowTime = System.currentTimeMillis()-time;
        log.info("MapperLoadHandler loaded，耗时【{}】毫秒，共加载mapper{}个", nowTime, mapperNum);
    }

    /**
     * 构建一个class对象
     * @param packages
     * @param modelName
     * @return
     * @throws CannotCompileException
     * @throws NotFoundException
     */
    public Class<?> buildClazz(String packages, String modelName) throws CannotCompileException, NotFoundException {
        log.debug("init Mapper packages[{}] modelName[{}]",packages, modelName);
        ClassPool pool = ClassPool.getDefault();
        CtClass baseMapperCt = pool.get(CommonBaseMapper.class.getName());
        CtClass mapperCt = pool.makeInterface(getBackPackage(packages) + PACKAGE_NAME + modelName + MAPPER_NAME, baseMapperCt);
        SignatureAttribute.ClassSignature sc = new SignatureAttribute.ClassSignature(null, null,
                new SignatureAttribute.ClassType[]{new SignatureAttribute.ClassType(baseMapperCt.getPackageName() +"."+ baseMapperCt.getSimpleName(),
                        new SignatureAttribute.TypeArgument[]{new SignatureAttribute.TypeArgument(
                                new SignatureAttribute.ClassType(packages + "." + modelName))}
                )});
        mapperCt.setGenericSignature(sc.encode());
        ConstPool constPool = mapperCt.getClassFile().getConstPool();
        AnnotationsAttribute annotationsAttribute = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
        annotationsAttribute.addAnnotation(new Annotation(Mapper.class.getName(), constPool));
        mapperCt.getClassFile().addAttribute(annotationsAttribute);
        return mapperCt.toClass(ClassUtils.getDefaultClassLoader(), mapperCt.getClass().getProtectionDomain());
    }

    private String[] getPackagesToScan(AnnotationMetadata metadata) {
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(
                metadata.getAnnotationAttributes(EnableAutoMapper.class.getName()));
        String[] basePackages = attributes.getStringArray("basePackages");
        if (basePackages.length == 0) {
            return new String[]{ClassUtils.getPackageName(metadata.getClassName())};
        }
        return basePackages;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }

    private String getBackPackage(String packages) {
        return packages.substring(0,packages.lastIndexOf("."));
    }

    public void setBasePackages(String[] basePackages) {
        this.basePackages = basePackages;
    }
}
