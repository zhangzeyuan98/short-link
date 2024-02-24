package com.zzy.shortlink.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzy.shortlink.project.common.constant.RedisKeyConstant;
import com.zzy.shortlink.project.common.convention.exception.ClientException;
import com.zzy.shortlink.project.common.convention.exception.ServiceException;
import com.zzy.shortlink.project.common.enums.VailDateTypeEnum;
import com.zzy.shortlink.project.config.GotoDomainWhiteListConfiguration;
import com.zzy.shortlink.project.dao.entity.ShortLinkDO;
import com.zzy.shortlink.project.dao.entity.ShortLinkGotoDO;
import com.zzy.shortlink.project.dao.entity.ShortLinkAccessStatsDTO;
import com.zzy.shortlink.project.dao.mapper.ShortLinkAccessStatsMapper;
import com.zzy.shortlink.project.dao.mapper.ShortLinkGotoMapper;
import com.zzy.shortlink.project.dao.mapper.ShortLinkMapper;
import com.zzy.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.zzy.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.zzy.shortlink.project.dto.req.ShortLinkUpdateReqDTO;
import com.zzy.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.zzy.shortlink.project.dto.resp.ShortLinkGroupCountQueryRespDTO;
import com.zzy.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import com.zzy.shortlink.project.mq.producer.DelayShortLinkStatsProducer;
import com.zzy.shortlink.project.mq.producer.ShortLinkStatsSaveProducer;
import com.zzy.shortlink.project.service.ShortLinkService;
import com.zzy.shortlink.project.toolkit.FunnelRateLimiterUtil;
import com.zzy.shortlink.project.toolkit.HashUtil;
import com.zzy.shortlink.project.toolkit.LinkUtil;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.zzy.shortlink.project.common.constant.RedisKeyConstant.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ShortLinkServiceImpl extends ServiceImpl<ShortLinkMapper, ShortLinkDO> implements ShortLinkService {

    /**
     * 布隆过滤器
     */
    private final RBloomFilter<String> shortUriCreateCachePenetrationBloomFilter;
    /**
     * 接口限流和熔断（令牌桶算法--基于redis、lua原子性）
     */
    private final RRateLimiter linkCreateFlowRiskControlFilter;

    /**
     * 接口限流和熔断（漏斗算法--基于本地内存）
     */
    private final FunnelRateLimiterUtil funnelRateLimiter;

    /**
     * Redis相关库引入
     */
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;


    /**
     * 数据库操作mapper
     */
    private final ShortLinkGotoMapper shortLinkGotoMapper;
    private final ShortLinkAccessStatsMapper shortLinkAccessStatsMapper;

    /**
     * Redis-stream消息队列
     */
    private final ShortLinkStatsSaveProducer shortLinkStatsSaveProducer;

    /**
     * 信息校验
     */
    private final GotoDomainWhiteListConfiguration gotoDomainWhiteListConfiguration;

    @Value("${short-link.goto-domain.domain-prefix}")
    private String restoreDomainPrefix;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParams) {
        // 跳转链接白名单验证
        verificationWhiteList(requestParams.getOriginUrl());
        if(requestParams.getValidDate().before(new Date())) {
            throw new ClientException("有效期设置错误");
        }

        String shortLinkSuffix = generateSuffix(requestParams);
        String fullShortUrl = requestParams.getDomain() + "/" + shortLinkSuffix;
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .shortUri(shortLinkSuffix)
                .domain(requestParams.getDomain())
                .createType(requestParams.getCreateType())
                .describe(requestParams.getDescribe())
                .enableStatus(0)
                .fullShortUrl(fullShortUrl)
                .originUrl(requestParams.getOriginUrl())
                .gid(requestParams.getGid())
                .validDateType(requestParams.getValidDateType())
                .validDate(requestParams.getValidDate())
                .clickNum(0)
                .build();
        shortLinkDO.setFullShortUrl(fullShortUrl);
        ShortLinkGotoDO shortLinkGotoDO = ShortLinkGotoDO.builder()
                .fullShortUrl(fullShortUrl)
                .gid(requestParams.getGid())
                .build();
        try {
            baseMapper.insert(shortLinkDO);
            shortLinkGotoMapper.insert(shortLinkGotoDO);
        } catch (DuplicateKeyException e) {
            // 在创建短链接的流程中，是先插入数据库，再将短链接放入布隆过滤器中，这两步操作存在一定延时
            // 因此在并发情况下，很有可能出现某个full_short_url已经插入数据库，但还未被放入布隆过滤器，
            // 另一个线程刚好创建同一个full_short_url，此时布隆过滤器中还没有该full_short_url，
            // 因此可以通过布隆过滤器的检验，最终造成主键插入重复的错误
            log.warn("短链接：{}重复入库", fullShortUrl);
            throw new ServiceException("短链接生成重复");
        }
        // 在创建短链接时，将短链接放入缓存中，进行缓存预热
        stringRedisTemplate.opsForValue().set(
                String.format(GOTO_SHORT_LINK_KEY, fullShortUrl),
                requestParams.getOriginUrl(),
                60L,
                TimeUnit.MINUTES
        );
        // 将生成的短链接放入布隆过滤器中
        shortUriCreateCachePenetrationBloomFilter.add(fullShortUrl);
        return ShortLinkCreateRespDTO.builder()
                .gid(requestParams.getGid())
                .originUrl(requestParams.getOriginUrl())
                .fullShortUrl("http://" + shortLinkDO.getFullShortUrl())
                .build();
    }

    @Override
    public IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParams) {
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getGid, requestParams.getGid())
                .eq(ShortLinkDO::getEnableStatus, 0)
                .eq(ShortLinkDO::getDelFlag, 0)
                .orderByDesc(ShortLinkDO::getCreateTime);
        IPage<ShortLinkDO> shortLinkDOList = baseMapper.selectPage(requestParams, queryWrapper);
        return shortLinkDOList.convert(each -> {
            ShortLinkPageRespDTO result = BeanUtil.toBean(each, ShortLinkPageRespDTO.class);
            result.setDomain("http://" + result.getDomain());
            return result;
        });
    }

    @Override
    public List<ShortLinkGroupCountQueryRespDTO> groupShortLinkCount(List<String> requestParams) {
        QueryWrapper<ShortLinkDO> queryWrapper = Wrappers.query(new ShortLinkDO())
                .select("gid as gid, count(*) as shortLinkCount")
                .in("gid", requestParams)
                .eq("enable_status", 0)
                .groupBy("gid");
        List<Map<String, Object>> shortLinkDOList = baseMapper.selectMaps(queryWrapper);
        return BeanUtil.copyToList(shortLinkDOList, ShortLinkGroupCountQueryRespDTO.class);
    }

    @SneakyThrows
    @Override
    public void restoreUrl(String shortUri, ServletRequest request, ServletResponse response) {
//        boolean isEnableAccess = linkCreateFlowRiskControlFilter.tryAcquire(100, TimeUnit.MILLISECONDS);
//        if(Objects.equals(isEnableAccess, Boolean.FALSE)) {
//            throw new ServiceException("请求频率过高，请求失败!!!!");
//        }

        String serverName = request.getServerName();
        String fullShortUrl = serverName + "/" + shortUri;
        //1. 查询Redis中是否存在指定数据
        String originalUrl = stringRedisTemplate.opsForValue().get(String.format(GOTO_SHORT_LINK_KEY, fullShortUrl));
        if(StrUtil.isNotBlank(originalUrl)) {
            ShortLinkAccessStatsDTO shortLinkAccessStatsDTO = buildShortLinkStats(fullShortUrl,null, request, response);
//            delayShortLinkStatsProducer.send(shortLinkAccessStatsDTO);
            shortLinkStats(fullShortUrl, null, shortLinkAccessStatsDTO);
            ((HttpServletResponse) response).sendRedirect(restoreDomainPrefix + originalUrl);
            return;
        }
        //2. 在布隆过滤器中查询数据库中是否存在指定数据，但是存在误判的可能--即数据库中没有的，但是布隆过滤器中判断存在
        boolean contains = shortUriCreateCachePenetrationBloomFilter.contains(fullShortUrl);
        if(!contains) {
            ((HttpServletResponse) response).sendRedirect("/page/notfound");
            return;
        }
        //3. 判断空值集合中是否存在对应的短链接，如果存在，则直接返回，代表数据库中不存在相应数据(此处根据情况启用)
        String gotoIsNullShortLink = stringRedisTemplate.opsForValue().get(String.format(RedisKeyConstant.GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl));
        if(StrUtil.isNotBlank(gotoIsNullShortLink)) {
            ((HttpServletResponse) response).sendRedirect("/page/notfound");
            return;
        }
        //4. 当缓存中都不存在时，获取对应短链接的分布式锁
        RLock lock = redissonClient.getLock(String.format(RedisKeyConstant.LOCK_GOTO_SHORT_LINK_KEY, fullShortUrl));
        lock.lock();
        try {
            //5. 二次缓存检查，保障缓存更新后能及时访问，以免积压的请求打到数据库
            originalUrl = stringRedisTemplate.opsForValue().get(String.format(GOTO_SHORT_LINK_KEY, fullShortUrl));
            if(StrUtil.isNotBlank(originalUrl)) {
                ShortLinkAccessStatsDTO shortLinkAccessStatsDTO = buildShortLinkStats(fullShortUrl,null, request, response);
//                delayShortLinkStatsProducer.send(shortLinkAccessStatsDTO);
                shortLinkStats(fullShortUrl, null, shortLinkAccessStatsDTO);
                ((HttpServletResponse) response).sendRedirect(restoreDomainPrefix + originalUrl);
                return;
            }
            //6. 根据短链接去路由表中查询其所对应的gid，因为t_link表是根据gid分表的，所以需要先查询到gid
            LambdaQueryWrapper<ShortLinkGotoDO> linkGotoDOLambdaQueryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                    .eq(ShortLinkGotoDO::getFullShortUrl, fullShortUrl);
            ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(linkGotoDOLambdaQueryWrapper);
            //7. 判断路由表中是否存在对应短链接
            if (shortLinkGotoDO == null) {
                // TODO 严谨来说需要风险控制
                //8. 如果不存在对应短链接，即当数据为null时，将指定的 fullShortUrl 存储到redis中，且value设置为null
                stringRedisTemplate.opsForValue().set(String.format(RedisKeyConstant.GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl), "-",30L, TimeUnit.SECONDS);
                ((HttpServletResponse) response).sendRedirect("/page/notfound");
                return;
            }
            //9. 根据gid和短链接去t_link表中查询指定数据
            LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                    .eq(ShortLinkDO::getFullShortUrl, fullShortUrl)
                    .eq(ShortLinkDO::getGid, shortLinkGotoDO.getGid())
                    .eq(ShortLinkDO::getEnableStatus, 0)
                    .eq(ShortLinkDO::getDelFlag, 0)
                    .orderByDesc(ShortLinkDO::getCreateTime);
            ShortLinkDO shortLinkDO = baseMapper.selectOne(queryWrapper);
            if(shortLinkDO == null || (shortLinkDO.getValidDate() != null && shortLinkDO.getValidDate().before(new Date()))) {
                // TODO 理论上时间过期的数据库记录，应该在查询时筛选掉
                stringRedisTemplate.opsForValue().set(String.format(RedisKeyConstant.GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl), "-",30L, TimeUnit.SECONDS);
                ((HttpServletResponse) response).sendRedirect("/page/notfound");
                return;
            }
            // 这里设置缓存的有效期到所指定的过期时间是存在一定问题的，因为理论上会将所有的短链接都刷到缓存中，这是没有必要的，理论上设置一个较短的过期时间即可
            // 当然设置较短的过期时间也存在一定的问题
            // 1. 对所有数据一视同仁，无法做到细致控制
            // 2. 短链接本身的有效时间和缓存中的过期时间的冲突
            // TODO 1. 针对热点Key，在设置缓存有效期时可以更长一些
            // TODO 2. 在尝试设置过期时间时，如果发现想要设置的过期时间大于短链接本身的有效时间时，则直接设置为本身的有效时间

            //10. 目前的处理：当查询到指定数据时，及时更新缓存并设置有效期到所指定的过期时间，同时跳转链接
            stringRedisTemplate.opsForValue().set(
                    String.format(GOTO_SHORT_LINK_KEY, fullShortUrl),
                    shortLinkDO.getOriginUrl(),
                    LinkUtil.getLinkCacheValidTime(shortLinkDO.getValidDate()),
                    TimeUnit.MILLISECONDS
            );
            ShortLinkAccessStatsDTO shortLinkAccessStatsDTO = buildShortLinkStats(fullShortUrl,null, request, response);
//            delayShortLinkStatsProducer.send(shortLinkAccessStatsDTO);
            shortLinkStats(fullShortUrl, null, shortLinkAccessStatsDTO);
            ((HttpServletResponse) response).sendRedirect(restoreDomainPrefix + shortLinkDO.getOriginUrl());
        } finally {
            //11. 释放对应分布式锁
            lock.unlock();
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateShortLink(ShortLinkUpdateReqDTO requestParam) {
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getGid, requestParam.getOriginGid())
                .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                .eq(ShortLinkDO::getDelFlag, 0)
                .eq(ShortLinkDO::getEnableStatus, 0);
        ShortLinkDO hasShortLinkDO = baseMapper.selectOne(queryWrapper);
        if (hasShortLinkDO == null) {
            throw new ClientException("待修改的短链接记录不存在");
        }
        //  修改不涉及gid，则直接在原表上操作即可
        if(Objects.equals(hasShortLinkDO.getGid(), requestParam.getGid())) {
            LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                    .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                    .eq(ShortLinkDO::getGid, requestParam.getGid())
                    .eq(ShortLinkDO::getDelFlag, 0)
                    .eq(ShortLinkDO::getEnableStatus, 0)
                    .set(Objects.equals(requestParam.getValidDateType(), VailDateTypeEnum.PERMANENT.getType()), ShortLinkDO::getValidDate, null);
            ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                    .domain(hasShortLinkDO.getDomain())
                    .shortUri(hasShortLinkDO.getShortUri())
                    .createType(hasShortLinkDO.getCreateType())
                    .gid(requestParam.getGid())
                    .originUrl(requestParam.getOriginUrl())
                    .describe(requestParam.getDescribe())
                    .validDateType(requestParam.getValidDateType())
                    .validDate(requestParam.getValidDate())
                    .build();
            baseMapper.update(shortLinkDO, updateWrapper);
        } else {
            RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(String.format(LOCK_GID_UPDATE_KEY, hasShortLinkDO.getFullShortUrl()));
            RLock rLock = readWriteLock.writeLock();
            if (!rLock.tryLock()) {
                throw new ServiceException("该短链接正在被访问，请稍后....");
            }

            try {
                // 删除原有表t_link中数据
                LambdaUpdateWrapper<ShortLinkDO> linkUpdateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                        .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(ShortLinkDO::getGid, hasShortLinkDO.getGid())
                        .eq(ShortLinkDO::getDelFlag, 0)
                        .eq(ShortLinkDO::getEnableStatus, 0);
                ShortLinkDO delShortLinkDO = ShortLinkDO.builder().build();
                delShortLinkDO.setDelFlag(1);
                baseMapper.update(delShortLinkDO, linkUpdateWrapper);
                // 重新插入新数据
                ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                        .originUrl(requestParam.getOriginUrl())
                        .gid(requestParam.getGid())
                        .validDateType(requestParam.getValidDateType())
                        .validDate(requestParam.getValidDate())
                        .describe(requestParam.getDescribe())
                        .createType(hasShortLinkDO.getCreateType())
                        .shortUri(hasShortLinkDO.getShortUri())
                        .enableStatus(hasShortLinkDO.getEnableStatus())
                        .fullShortUrl(hasShortLinkDO.getFullShortUrl())
                        .clickNum(hasShortLinkDO.getClickNum())
                        .domain(hasShortLinkDO.getDomain())
                        .build();
                baseMapper.insert(shortLinkDO);

                // 删除原有表t_link_goto中数据
                LambdaQueryWrapper<ShortLinkGotoDO> linkGotoQueryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                        .eq(ShortLinkGotoDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(ShortLinkGotoDO::getGid, hasShortLinkDO.getGid());
                ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(linkGotoQueryWrapper);
                shortLinkGotoMapper.deleteById(shortLinkGotoDO.getId());
                // 更新表t_link_goto中数据
                shortLinkGotoDO.setGid(requestParam.getGid());
                shortLinkGotoMapper.insert(shortLinkGotoDO);

                LambdaUpdateWrapper<ShortLinkAccessStatsDTO> linkAccessStatsUpdateWrapper = Wrappers.lambdaUpdate(ShortLinkAccessStatsDTO.class)
                        .eq(ShortLinkAccessStatsDTO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(ShortLinkAccessStatsDTO::getGid, hasShortLinkDO.getGid())
                        .eq(ShortLinkAccessStatsDTO::getDelFlag, 0);
                ShortLinkAccessStatsDTO linkAccessStatsDO = ShortLinkAccessStatsDTO.builder()
                        .gid(requestParam.getGid())
                        .build();

                // 表t_link_access_stats不是按照gid进行分片的，所以直接进行更新即可
                shortLinkAccessStatsMapper.update(linkAccessStatsDO, linkAccessStatsUpdateWrapper);
            } finally {
                rLock.unlock();
            }
        }
        // 执行更新操作后一律将缓存清空
        stringRedisTemplate.delete(String.format(GOTO_SHORT_LINK_KEY, requestParam.getFullShortUrl()));
        stringRedisTemplate.delete(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, requestParam.getFullShortUrl()));
    }

    public ShortLinkAccessStatsDTO buildShortLinkStats(String fullShortUrl, String gid,  ServletRequest request, ServletResponse response) {
        // TODO 1. 目前UV的统计依赖于Cookie的传递，这无法识别移动端，后续可能会通过jwt改进
        // TODO(已完成，已经使用HyperLogLog实现UV和UIP统计) 2. 老访客/IP的识别依赖于Redis中的set集合，这可能会将Redis的内存撑爆，后续必须优化
        // TODO 3. 不应该每次请求都直接访问数据库进行统计，应该通过批量操作，成段修改数据库，将使用futureTask/MQ消息队列优化

        // UV统计
        Cookie[] cookies = ((HttpServletRequest) request).getCookies();
        try {
            Date date = DateUtil.date();
            int hour = DateUtil.hour(date, true);
            int weekValue = DateUtil.dayOfWeekEnum(date).getIso8601Value();
            String formatTime = DateUtil.format(date, "yyyy-MM-dd");

            // PV统计
//            stringRedisTemplate.opsForValue().increment("short-link:stats:pv:" + formatTime + ":" + hour + fullShortUrl);
            redissonClient.getAtomicLong("short-link:stats:pv:" + formatTime + ":" + hour + fullShortUrl).incrementAndGet();

            // UV统计
            Runnable addResponseCookieTask = () -> {
                String uv = UUID.fastUUID().toString();
                Cookie uvCookie = new Cookie("uv", uv);
                uvCookie.setMaxAge(60 * 60 * 24 * 30);
                uvCookie.setPath(StrUtil.sub(fullShortUrl, fullShortUrl.indexOf("/"), fullShortUrl.length()));
                ((HttpServletResponse) response).addCookie(uvCookie);
                redissonClient.getHyperLogLog("short-link:stats:uv:" + formatTime + ":" + hour + fullShortUrl).add(uv);
                redissonClient.getHyperLogLog("short-link:stats:uv:" + formatTime + ":" + hour + fullShortUrl).mergeWith();
            };
            if(ArrayUtil.isNotEmpty(cookies)) {
                Arrays.stream(cookies)
                        .filter(each -> Objects.equals(each.getName(), "uv"))
                        .findFirst()
                        .map(Cookie::getValue)
                        .ifPresentOrElse(each -> {
                            redissonClient.getHyperLogLog("short-link:stats:uv:" + formatTime + ":" + hour + fullShortUrl).add(each);
                        }, addResponseCookieTask);
            } else {
                addResponseCookieTask.run();
            }

            // UIP统计
            String remoteAddr = request.getRemoteAddr();
            redissonClient.getHyperLogLog("short-link:stats:uip:" + formatTime + ":" + hour + fullShortUrl).add(remoteAddr);

            return ShortLinkAccessStatsDTO
                    .builder()
                    .hour(hour)
                    .gid(gid)
                    .weekday(weekValue)
                    .fullShortUrl(fullShortUrl)
                    .date(date)
                    .build();
        }catch (Throwable ex) {
            log.error("短链接访问量实体创建异常", ex);
        }
        return null;
    }

    @Override
    public void shortLinkStats(String fullShortUrl, String gid, ShortLinkAccessStatsDTO statsRecord) {
        Map<String, String> producerMap = new HashMap<>();
        producerMap.put("fullShortUrl", fullShortUrl);
        producerMap.put("gid", gid);
        producerMap.put("statsRecord", JSON.toJSONString(statsRecord));
        shortLinkStatsSaveProducer.send(producerMap);
    }


    private String generateSuffix(ShortLinkCreateReqDTO requestParams) {
        String originUrl = requestParams.getOriginUrl();
        int customGenerateCount = 0;
        String shortUri = null;
        while(true) {
            if(customGenerateCount > 10) {
                throw new ServiceException("短链接频繁生成，请稍后再试");
            }
            originUrl += UUID.randomUUID().toString();
            shortUri = HashUtil.hashToBase62(originUrl);
            if(!shortUriCreateCachePenetrationBloomFilter.contains(requestParams.getDomain() + "/" + shortUri)) {
                break;
            }
            customGenerateCount++;
        }
        return shortUri;
    }

    private void verificationWhiteList(String originUrl) {
        Boolean enable = gotoDomainWhiteListConfiguration.getEnable();
        if(enable == null || !enable) {
            return;
        }

        List<String> details = gotoDomainWhiteListConfiguration.getDetails();
        if(!details.contains(originUrl)) {
            throw new ClientException("您所跳转的链接可能存在问题，请按照以下名单生成的=跳转链接: " + gotoDomainWhiteListConfiguration.getNames());
        }
    }

    private String slidingWindowLimiter(String key) {
//        RBatch batch = redissonClient.createBatch();
//        long now = DateUtil.date().getTime();
//        batch.getScoredSortedSet(key).addAsync(now, StrUtil.toString(now));
//        RFuture<Integer> scoredSortedSet = batch.getScoredSortedSet(key).removeRangeByScoreAsync(now - 100000, Boolean.TRUE, now, Boolean.TRUE);
//        scoredSortedSet.get();
//        if (!scoredSortedSet.isExists()) {
//            return;
//        }
//        batch.execute();
        return null;
    }

    private boolean funnelRateLimiter(String key, String username) {
        return funnelRateLimiter.isActionAllowed(username, key, 20, 2);
    }

    public void test() {

    }
}
