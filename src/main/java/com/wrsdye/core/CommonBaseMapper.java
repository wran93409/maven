package com.wrsdye.core;

import com.baomidou.mybatisplus.core.enums.SqlMethod;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.*;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.wrsdye.core.utils.MapperSqlHelper;
import com.google.common.collect.Maps;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.session.SqlSession;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * @author wangrx
 * @description GeneralCommonMapper
 * @date 2021/10/29 10:20 上午
 */
public interface CommonBaseMapper<T> extends BaseMapper<T>{

    Log log = LogFactory.getLog(CommonBaseMapper.class);

    int DEFAULT_BATCH_SIZE = 1000;

    Map<Object,Class<?>> entityClassMap = Maps.newHashMap();

    Map<Object,Class<?>> mapperClassMap = Maps.newHashMap();

    /**
     * 链式查询 lambda 式
     * <p>注意：不支持 Kotlin </p>
     *
     * @return LambdaQueryWrapper 的包装类
     */
    default LambdaQueryChainWrapper<T> lambdaQuery() {
        return ChainWrappers.lambdaQueryChain(this);
    }


    /**
     * 链式更改 lambda 式
     * <p>注意：不支持 Kotlin </p>
     *
     * @return LambdaUpdateWrapper 的包装类
     */
    default LambdaUpdateChainWrapper<T> lambdaUpdate() {
        return ChainWrappers.lambdaUpdateChain(this);
    }


    /**
     * 插入一条记录（选择字段，策略插入）
     *
     * @param entity 实体对象
     */
    default boolean save(T entity) {
        return SqlHelper.retBool(this.insert(entity));
    }

    /**
     * 根据 ID 选择修改
     *
     * @param entity 实体对象
     */
    default boolean update(T entity) {
        return SqlHelper.retBool(this.updateById(entity));
    }


    @Transactional(rollbackFor = Exception.class)
    default boolean saveBatch(Collection<T> entityList) {
        return saveBatch(entityList,DEFAULT_BATCH_SIZE);
    }


    @Transactional(rollbackFor = Exception.class)
    default boolean saveBatch(Collection<T> entityList, int batchSize) {
        String sqlStatement = MapperSqlHelper.getSqlStatement(getMapper(), SqlMethod.INSERT_ONE);
        return executeBatch(entityList, batchSize, (sqlSession, entity) -> sqlSession.insert(sqlStatement, entity));
    }



    @Transactional(rollbackFor = Exception.class)
    default boolean updateBatchById(Collection<T> entityList) {
        return updateBatchById(entityList,DEFAULT_BATCH_SIZE);
    }

    @Transactional(rollbackFor = Exception.class)
    default boolean updateBatchById(Collection<T> entityList, int batchSize) {
        String sqlStatement = MapperSqlHelper.getSqlStatement(getMapper(), SqlMethod.UPDATE_BY_ID);
        return executeBatch(entityList, batchSize, (sqlSession, entity) -> {
            MapperMethod.ParamMap<T> param = new MapperMethod.ParamMap<>();
            param.put(Constants.ENTITY, entity);
            sqlSession.update(sqlStatement, param);
        });
    }

    /**
     * TableId 注解存在更新记录，否插入一条记录
     *
     * @param entity 实体对象
     * @return boolean
     */
    @Transactional(rollbackFor = Exception.class)
    default boolean saveOrUpdate(T entity) {
        if (null != entity) {
            TableInfo tableInfo = TableInfoHelper.getTableInfo(this.currentModelClass());
            Assert.notNull(tableInfo, "error: can not execute. because can not find cache of TableInfo for entity!");
            String keyProperty = tableInfo.getKeyProperty();
            Assert.notEmpty(keyProperty, "error: can not execute. because can not find column for id from entity!");
            Object idVal = ReflectionKit.getFieldValue(entity, tableInfo.getKeyProperty());
            return StringUtils.checkValNull(idVal) || Objects.isNull(this.selectById((Serializable) idVal)) ? save(entity) : update(entity);
        }
        return false;
    }

    @Transactional(rollbackFor = Exception.class)
    default boolean saveOrUpdateBatch(Collection<T> entityList, int batchSize) {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(currentModelClass());
        Assert.notNull(tableInfo, "error: can not execute. because can not find cache of TableInfo for entity!");
        String keyProperty = tableInfo.getKeyProperty();
        Assert.notEmpty(keyProperty, "error: can not execute. because can not find column for id from entity!");
        return MapperSqlHelper.saveOrUpdateBatch(currentModelClass(), getMapper(), log, entityList, batchSize, (sqlSession, entity) -> {
            Object idVal = ReflectionKit.getFieldValue(entity, keyProperty);
            return StringUtils.checkValNull(idVal)
                    || CollectionUtils.isEmpty(sqlSession.selectList(MapperSqlHelper.getSqlStatement(getMapper(),SqlMethod.SELECT_BY_ID), entity));
        }, (sqlSession, entity) -> {
            MapperMethod.ParamMap<T> param = new MapperMethod.ParamMap<>();
            param.put(Constants.ENTITY, entity);
            sqlSession.update(MapperSqlHelper.getSqlStatement(getMapper(),SqlMethod.UPDATE_BY_ID), param);
        });
    }

    /**
     * 执行批量操作
     *
     * @param list      数据集合
     * @param batchSize 批量大小
     * @param consumer  执行方法
     * @param <E>       泛型
     * @return 操作结果
     * @since 3.3.1
     */
    default  <E> boolean executeBatch(Collection<E> list, int batchSize, BiConsumer<SqlSession, E> consumer) {
        return MapperSqlHelper.executeBatch(currentModelClass(), log, list, batchSize, consumer);
    }

    default Class<?>  currentModelClass(){
        if(entityClassMap.get(this)!=null){
            return entityClassMap.get(this);
        }
        ParameterizedType parameterizedType= (ParameterizedType) this.getMapper().getGenericInterfaces()[0];
        Class<?> clazz = (Class) parameterizedType.getActualTypeArguments()[0];
        entityClassMap.put(this,clazz);
        return clazz;
    }


    /**
     * 获取 目标对象
     * @return
     * @throws Exception
     */
    default Class<?> getMapper()  {
        if(mapperClassMap.get(this)!=null){
            return mapperClassMap.get(this);
        }
        try {
            Field h = this.getClass().getSuperclass().getDeclaredField("h");
            h.setAccessible(true);
            Object o1 = h.get(this);
            Field mapperInterface = o1.getClass().getDeclaredField("mapperInterface");
            mapperInterface.setAccessible(true);
            Class<?> clazz = (Class<?>) mapperInterface.get(o1);
            mapperClassMap.put(this,clazz);
            return clazz;
        }catch (Exception e){
            log.error("",e);
        }
        return null;
    }
}
