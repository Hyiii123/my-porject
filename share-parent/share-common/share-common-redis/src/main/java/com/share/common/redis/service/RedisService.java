package com.share.common.redis.service;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Component;

/**
 * spring redis 工具类
 *
 * @author share
 **/
@SuppressWarnings(value = { "unchecked", "rawtypes" })
@Component
public class RedisService
{
    @Autowired
    public RedisTemplate redisTemplate;

    /**
     * 缓存基本的对象，Integer、String、实体类等
     *
     * @param key 缓存的键值
     * @param value 缓存的值
     */
    public <T> void setCacheObject(final String key, final T value)
    {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 缓存基本的对象，Integer、String、实体类等
     *
     * @param key 缓存的键值
     * @param value 缓存的值
     * @param timeout 时间
     * @param timeUnit 时间颗粒度
     */
    public <T> void setCacheObject(final String key, final T value, final Long timeout, final TimeUnit timeUnit)
    {
        redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }

    /**
     * 原子递增计数，并在第一次创建时设置过期时间。
     *
     * <p>适用于限流、短时统计等场景，避免业务代码执行“读取-加一-写回”时产生并发覆盖。</p>
     */
    public long increment(final String key, final long timeout, final TimeUnit timeUnit)
    {
        Long value = redisTemplate.opsForValue().increment(key, 1L);
        if (value != null && value == 1L && timeout > 0)
        {
            redisTemplate.expire(key, timeout, timeUnit);
        }
        return value == null ? 0L : value;
    }

    /**
     * 设置有效时间
     *
     * @param key Redis键
     * @param timeout 超时时间
     * @return true=设置成功；false=设置失败
     */
    public boolean expire(final String key, final long timeout)
    {
        return expire(key, timeout, TimeUnit.SECONDS);
    }

    /**
     * 设置有效时间
     *
     * @param key Redis键
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return true=设置成功；false=设置失败
     */
    public boolean expire(final String key, final long timeout, final TimeUnit unit)
    {
        return redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 获取有效时间
     *
     * @param key Redis键
     * @return 有效时间
     */
    public long getExpire(final String key)
    {
        return redisTemplate.getExpire(key);
    }

    /**
     * 判断 key是否存在
     *
     * @param key 键
     * @return true 存在 false不存在
     */
    public Boolean hasKey(String key)
    {
        return redisTemplate.hasKey(key);
    }

    /**
     * 获得缓存的基本对象。
     *
     * @param key 缓存键值
     * @return 缓存键值对应的数据
     */
    public <T> T getCacheObject(final String key)
    {
        ValueOperations<String, T> operation = redisTemplate.opsForValue();
        return operation.get(key);
    }

    /**
     * 删除单个对象
     *
     * @param key
     */
    public boolean deleteObject(final String key)
    {
        return redisTemplate.delete(key);
    }

    /**
     * 删除集合对象
     *
     * @param collection 多个对象
     * @return
     */
    public boolean deleteObject(final Collection collection)
    {
        return redisTemplate.delete(collection) > 0;
    }

    /**
     * 缓存List数据
     *
     * @param key 缓存的键值
     * @param dataList 待缓存的List数据
     * @return 缓存的对象
     */
    public <T> long setCacheList(final String key, final List<T> dataList)
    {
        Long count = redisTemplate.opsForList().rightPushAll(key, dataList);
        return count == null ? 0 : count;
    }

    /**
     * 获得缓存的list对象
     *
     * @param key 缓存的键值
     * @return 缓存键值对应的数据
     */
    public <T> List<T> getCacheList(final String key)
    {
        return redisTemplate.opsForList().range(key, 0, -1);
    }

    /**
     * 缓存Set
     *
     * @param key 缓存键值
     * @param dataSet 缓存的数据
     * @return 缓存数据的对象
     */
    public <T> BoundSetOperations<String, T> setCacheSet(final String key, final Set<T> dataSet)
    {
        BoundSetOperations<String, T> setOperation = redisTemplate.boundSetOps(key);
        Iterator<T> it = dataSet.iterator();
        while (it.hasNext())
        {
            setOperation.add(it.next());
        }
        return setOperation;
    }

    /**
     * 获得缓存的set
     *
     * @param key
     * @return
     */
    public <T> Set<T> getCacheSet(final String key)
    {
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * 缓存Map
     *
     * @param key
     * @param dataMap
     */
    public <T> void setCacheMap(final String key, final Map<String, T> dataMap)
    {
        if (dataMap != null) {
            redisTemplate.opsForHash().putAll(key, dataMap);
        }
    }

    /**
     * 获得缓存的Map
     *
     * @param key
     * @return
     */
    public <T> Map<String, T> getCacheMap(final String key)
    {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * 往Hash中存入数据
     *
     * @param key Redis键
     * @param hKey Hash键
     * @param value 值
     */
    public <T> void setCacheMapValue(final String key, final String hKey, final T value)
    {
        redisTemplate.opsForHash().put(key, hKey, value);
    }

    /**
     * 获取Hash中的数据
     *
     * @param key Redis键
     * @param hKey Hash键
     * @return Hash中的对象
     */
    public <T> T getCacheMapValue(final String key, final String hKey)
    {
        HashOperations<String, String, T> opsForHash = redisTemplate.opsForHash();
        return opsForHash.get(key, hKey);
    }

    /**
     * 获取多个Hash中的数据
     *
     * @param key Redis键
     * @param hKeys Hash键集合
     * @return Hash对象集合
     */
    public <T> List<T> getMultiCacheMapValue(final String key, final Collection<Object> hKeys)
    {
        return redisTemplate.opsForHash().multiGet(key, hKeys);
    }

    /**
     * 删除Hash中的某条数据
     *
     * @param key Redis键
     * @param hKey Hash键
     * @return 是否成功
     */
    public boolean deleteCacheMapValue(final String key, final String hKey)
    {
        return redisTemplate.opsForHash().delete(key, hKey) > 0;
    }

    /**
     * 获得缓存的基本对象列表
     *
     * @param pattern 字符串前缀
     * @return 对象列表
     */
    public Collection<String> keys(final String pattern)
    {
        return redisTemplate.keys(pattern);
    }

    /**
     * 原子递减
     *
     * @param key 缓存键
     * @param delta 递减值
     * @return 递减后的数值
     */
    public long decrement(final String key, final long delta)
    {
        Long value = redisTemplate.opsForValue().decrement(key, delta);
        return value == null ? 0L : value;
    }

    /**
     * 原子递减（默认步长1）
     *
     * @param key 缓存键
     * @return 递减后的数值
     */
    public long decrement(final String key)
    {
        return decrement(key, 1L);
    }

    /**
     * 分布式锁 / 防重互斥写入 (setNX)
     *
     * @param key 缓存键
     * @param value 缓存值
     * @param timeout 超时时间
     * @param timeUnit 时间单位
     * @return true=获取成功并写入；false=已存在
     */
    public <T> Boolean setCacheObjectIfAbsent(final String key, final T value, final Long timeout, final TimeUnit timeUnit)
    {
        return redisTemplate.opsForValue().setIfAbsent(key, value, timeout, timeUnit);
    }

    /**
     * 向 Set 集合中添加成员
     */
    public Long sAdd(final String key, final Object... values)
    {
        return redisTemplate.opsForSet().add(key, values);
    }

    /**
     * 判断元素是否为 Set 集合成员
     */
    public Boolean sIsMember(final String key, final Object value)
    {
        return redisTemplate.opsForSet().isMember(key, value);
    }

    /**
     * 从 Set 集合中移除成员
     */
    public Long sRemove(final String key, final Object... values)
    {
        return redisTemplate.opsForSet().remove(key, values);
    }

    /**
     * 获取 Set 集合成员数量
     */
    public Long sCard(final String key)
    {
        return redisTemplate.opsForSet().size(key);
    }

    /**
     * 向 Sorted Set (ZSet) 添加成员与分值
     */
    public Boolean zAdd(final String key, final Object value, final double score)
    {
        return redisTemplate.opsForZSet().add(key, value, score);
    }

    /**
     * 原子增加 ZSet 成员分值
     */
    public Double zIncrementScore(final String key, final Object value, final double delta)
    {
        return redisTemplate.opsForZSet().incrementScore(key, value, delta);
    }

    /**
     * 倒序获取 ZSet 成员列表（带分值）
     */
    public Set<TypedTuple<Object>> zReverseRangeWithScores(final String key, final long start, final long end)
    {
        return redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);
    }

    /**
     * 倒序获取 ZSet 成员列表
     */
    public Set<Object> zReverseRange(final String key, final long start, final long end)
    {
        return redisTemplate.opsForZSet().reverseRange(key, start, end);
    }

    /**
     * 获取 ZSet 成员当前分值
     */
    public Double zScore(final String key, final Object value)
    {
        return redisTemplate.opsForZSet().score(key, value);
    }

    /**
     * 获取 ZSet 集合元素总数
     */
    public Long zCard(final String key)
    {
        return redisTemplate.opsForZSet().zCard(key);
    }
}
