package com.zzy.shortlink.project.mq.consumer;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zzy.shortlink.project.common.convention.exception.ServiceException;
import com.zzy.shortlink.project.dao.entity.ShortLinkAccessStatsDTO;
import com.zzy.shortlink.project.dao.entity.ShortLinkGotoDO;
import com.zzy.shortlink.project.dao.mapper.ShortLinkAccessStatsMapper;
import com.zzy.shortlink.project.dao.mapper.ShortLinkGotoMapper;
import com.zzy.shortlink.project.mq.producer.DelayShortLinkStatsProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.Computable;
import org.redisson.api.*;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.LongStream;

import static com.zzy.shortlink.project.common.constant.RedisKeyConstant.LOCK_GID_UPDATE_KEY;

@Component
@Slf4j
@RequiredArgsConstructor
public class ShortLinkStatsSaveConsumer implements StreamListener<String, MapRecord<String, String, String>> {

    private final RedissonClient redissonClient;

    private final StringRedisTemplate stringRedisTemplate;

    private final DelayShortLinkStatsProducer delayShortLinkStatsProducer;

    /**
     * 数据库操作mapper
     */
    private final ShortLinkGotoMapper shortLinkGotoMapper;
    private final ShortLinkAccessStatsMapper shortLinkAccessStatsMapper;

    private final ExecutorService mysqlComputeTaskExecutor;

    private ConcurrentHashMap<String, List<ShortLinkAccessStatsDTO>> statsCacheMap;

    @Override
    public void onMessage(MapRecord<String, String, String> message) {

        String stream = message.getStream();
        RecordId id = message.getId();
        try {
            Map<String, String> producerMap = message.getValue();
            String fullShortUrl = producerMap.get("fullShortUrl");
            if (StrUtil.isNotBlank(fullShortUrl)) {
                String gid = producerMap.get("gid");
                ShortLinkAccessStatsDTO statsRecord = JSON.parseObject(producerMap.get("statsRecord"), ShortLinkAccessStatsDTO.class);
                executeCompletableTask(fullShortUrl, gid, statsRecord);
//                oneShortLinkStats(fullShortUrl, gid, statsRecord);
            }
            stringRedisTemplate.opsForStream().delete(Objects.requireNonNull(stream), id.getValue());
        } catch (Throwable ex) {
            // 某某某情况宕机了
            log.error("记录短链接监控消费异常", ex);
        }
    }

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void executeCompletableTask(String fullShortUrl, String gid, ShortLinkAccessStatsDTO shortLinkAccessStatsDTO) throws ExecutionException, InterruptedException {
        long start = System.currentTimeMillis();
        CompletableFuture<Void> startTask = CompletableFuture.runAsync(() -> {
            System.out.println("当前线程：" + Thread.currentThread().getId());
            int i = 10 / 2;
            System.out.println("运行结果：" + i);
//            throw new ServiceException("故障测试");
        }, mysqlComputeTaskExecutor);

        CompletableFuture<Void> stateTask = CompletableFuture.runAsync(() -> {
            oneShortLinkStats(fullShortUrl, gid, shortLinkAccessStatsDTO);
        }, mysqlComputeTaskExecutor);

        CompletableFuture.allOf(stateTask, startTask).get();
        long end = System.currentTimeMillis();
        System.out.println("sql耗时:" + (end - start));
    }

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void oneShortLinkStats(String fullShortUrl, String gid, ShortLinkAccessStatsDTO shortLinkAccessStatsDTO) {
        fullShortUrl = Optional.ofNullable(fullShortUrl).orElse(shortLinkAccessStatsDTO.getFullShortUrl());
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(String.format(LOCK_GID_UPDATE_KEY, fullShortUrl));
        RLock rLock = readWriteLock.readLock();
        if (!rLock.tryLock()) {
            delayShortLinkStatsProducer.send(shortLinkAccessStatsDTO);
            return;
        }
        try {
            // TODO: 想要保证读取数据在时刻上的强一致性，可以考虑使用Lua脚本
            String date = DateUtil.format(shortLinkAccessStatsDTO.getDate(), "yyyy-MM-dd");
            String keyBackEnd =  date + ":" + shortLinkAccessStatsDTO.getHour().toString() + fullShortUrl;

            RBatch rBatch = redissonClient.createBatch();
            rBatch.getAtomicLong("short-link:stats:pv:" + keyBackEnd).getAsync();
            rBatch.getHyperLogLog("short-link:stats:uv:" + keyBackEnd).countAsync();
            rBatch.getHyperLogLog("short-link:stats:uip:" + keyBackEnd).countAsync();
            // 执行RBatch中的全部命令，并返回执行结果
            BatchResult<?> result = rBatch.execute();
            List<?> responses = result.getResponses();

            // 如果存在一个数据访问错误，则直接返回
            if(responses.size() != 3 && !ArrayUtil.isNotEmpty(responses)) {
                return;
            }

            shortLinkAccessStatsDTO.setPv((Long) responses.get(0));
            shortLinkAccessStatsDTO.setUv((Long) responses.get(1));
            shortLinkAccessStatsDTO.setUip((Long) responses.get(2));

            // 获取最新的gid
            if(StrUtil.isBlank(gid)) {
                LambdaQueryWrapper<ShortLinkGotoDO> linkGotoDOLambdaQueryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                        .eq(ShortLinkGotoDO::getFullShortUrl, fullShortUrl);
                ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(linkGotoDOLambdaQueryWrapper);
                gid = shortLinkGotoDO.getGid();
            }
            shortLinkAccessStatsDTO.setGid(gid);

            // TODO: 尝试实现批量SQL的执行
            // 方案一：Redis List/ZSet
//            RLock lock = redissonClient.getLock("short-link:stats:lock:" + keyBackEnd);
//            lock.lock();
//            try {
//                ShortLinkAccessStatsDTO res = (ShortLinkAccessStatsDTO) redissonClient.getBucket("short-link:stats:cache:" + keyBackEnd).get();
//                if(!Objects.isNull(res)) {
//                    if(shortLinkAccessStatsDTO.getPv() - res.getPv() > 2)
//                        actualSQL(shortLinkAccessStatsDTO);
//                }
//                redissonClient.getBucket("short-link:stats:cache:" + keyBackEnd).set(shortLinkAccessStatsDTO);
//            } finally {
//                lock.unlock();
//            }

            // 方案二：concurrentHashMap
//            statsCacheMap.computeIfAbsent(shortLinkAccessStatsDTO.getFullShortUrl(), (k ,v) -> {});
            actualSQL(shortLinkAccessStatsDTO);

        }catch (Throwable ex) {
            log.error("短链接访问量统计异常", ex);
        } finally {
            rLock.unlock();
        }
    }

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void actualSQL(ShortLinkAccessStatsDTO shortLinkAccessStatsDTO) {
        // 执行更新操作
        shortLinkAccessStatsMapper.shortLinkStats(shortLinkAccessStatsDTO);
    }
}
