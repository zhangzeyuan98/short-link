package com.zzy.shortlink.project.toolkit;

import cn.hutool.core.bean.BeanUtil;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBatch;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.protocol.ScoredEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;


@Component
@RequiredArgsConstructor
public class RedissonZSetUtil {

    private final RedissonClient client;

    public static final String DEFAULT_SCORE_KEY = "default";

    /**
     * 默认保存时间
     */
    private static final long DEFAULT_EXPIRE_TIME_SECONDS = 3600L;

    /**
     * 新增ZSet元素,存在则刷新
     *
     * @param refreshExpire 过期时间,不为null则重新赋值
     */
    public <T> void zscoreAddAsync(String key, double score, T member, Long refreshExpire) {
        RScoredSortedSet<Object> scoredSortedSet = client.getScoredSortedSet(key);
        if (null != refreshExpire) {
            scoredSortedSet.expire(Duration.ofSeconds(DEFAULT_EXPIRE_TIME_SECONDS));
        }
        scoredSortedSet.addAsync(score, member);
    }

    /**
     * 批量新增
     */
    public <T> void zScoreAddAsyncBatch(String key, Map<String, Double> map, long seconds) {
        if (seconds <= 0) {
            seconds = DEFAULT_EXPIRE_TIME_SECONDS;
        }
        RScoredSortedSet<Object> scoredSortedSet = client.getScoredSortedSet(key);
        // 只能针对 key 设置过期时间，zset 中的元素不能单独设置.
        scoredSortedSet.add(0, DEFAULT_SCORE_KEY);
        scoredSortedSet.expire(Duration.ofSeconds(seconds));
        RBatch batch = client.createBatch();
        map.forEach((member, score) -> {
            batch.getScoredSortedSet(key).addAsync(score, member);
        });
        batch.execute();
    }

    /**
     * 读取指定 key 下所有 member, 按照 score 升序(默认)
     */
    public Collection<Object> getZSetMembers(String key, int startIndex, int endIndex) {
        RScoredSortedSet<Object> scoredSortedSet = client.getScoredSortedSet(key);
        return scoredSortedSet.valueRange(startIndex, endIndex);
    }

    /**
     * 取指定 key 下所有 member, 按照 score 降序
     */
    public Collection<Object> getZSetMembersReversed(String key, int startIndex, int endIndex) {
        RScoredSortedSet<Object> scoredSortedSet = client.getScoredSortedSet(key);
        return scoredSortedSet.valueRangeReversed(startIndex, endIndex);
    }

    /**
     * 读取 member和score, 按照 score 升序(默认)
     */
    public Collection<ScoredEntry<Object>> getZSetEntryRange(String key, int startIndex, int endIndex) {
        RScoredSortedSet<Object> scoredSortedSet = client.getScoredSortedSet(key);
        return scoredSortedSet.entryRange(startIndex, endIndex);
    }

    /**
     * 读取 member和score, 按照 score 降序
     */
    public Collection<ScoredEntry<Object>> getZSetEntryRangeReversed(String key, int startIndex, int endIndex) {
        RScoredSortedSet<Object> scoredSortedSet = client.getScoredSortedSet(key);
        return scoredSortedSet.entryRangeReversed(startIndex, endIndex);
    }

    /**
     * 读取指定 key 下 member 的 score
     * 返回null 表示不存在
     */
    public Double getZSetMemberScore(String key, String member) {
        RScoredSortedSet<Object> scoredSortedSet = client.getScoredSortedSet(key);
        if (!scoredSortedSet.isExists()) {
            return null;
        }
        return scoredSortedSet.getScore(member);
    }


    /**
     * 读取指定 key 下 memberList 的 score
     * 返回null 表示不存在
     */
    public Double getZSetMemberScore(String key, List<String> memberList) {
        RScoredSortedSet<Object> scoredSortedSet = client.getScoredSortedSet(key);
        if (!scoredSortedSet.isExists()) {
            return null;
        }
        return scoredSortedSet.getScore(memberList);
    }

    /**
     * 读取指定 key 下 member 的 rank 排名(升序情况)
     * 返回null 表示不存在, 下标从0开始
     */
    public Integer getZSetMemberRank(String key, String member) {
        RScoredSortedSet<Object> scoredSortedSet = client.getScoredSortedSet(key);
        if (!scoredSortedSet.isExists()) {
            return null;
        }
        return scoredSortedSet.rank(member);
    }


    /**
     * 异步删除指定 ZSet 中的指定 memberName 元素
     */
    public void removeZSetMemberAsync(String key, String memberName) {
        RScoredSortedSet<Object> scoredSortedSet = client.getScoredSortedSet(key);
        if (!scoredSortedSet.isExists()) {
            return;
        }
        scoredSortedSet.removeAsync(memberName);
    }


    /**
     * 异步批量删除指定 ZSet 中的指定 member 元素列表
     */
    public void removeZSetMemberAsync(String key, List<String> memberList) {
        RScoredSortedSet<Object> scoredSortedSet = client.getScoredSortedSet(key);
        if (!scoredSortedSet.isExists()) {
            return;
        }
        RBatch batch = client.createBatch();
        memberList.forEach(member -> batch.getScoredSortedSet(key).removeAsync(member));
        batch.execute();
    }


    /**
     * 统计ZSet分数范围内元素总数. 区间包含分数本身
     * 注意这里不能用 -1 代替最大值
     */
    public int getZSetCountByScoresInclusive(String key, double startScore, double endScore) {
        RScoredSortedSet<Object> scoredSortedSet = client.getScoredSortedSet(key);
        if (!scoredSortedSet.isExists()) {
            return 0;
        }
        return scoredSortedSet.count(startScore, true, endScore, true);
    }

    /**
     * 返回ZSet分数范围内 member 列表. 区间包含分数本身.
     * 注意这里不能用 -1 代替最大值
     */
    public Collection<Object> getZSetMembersByScoresInclusive(String key, double startScore, double endScore) {
        RScoredSortedSet<Object> scoredSortedSet = client.getScoredSortedSet(key);
        if (!scoredSortedSet.isExists()) {
            return null;
        }
        return scoredSortedSet.valueRange(startScore, true, endScore, true);
    }

    /**
     * 获取所有的指定前缀 keys
     */
    public Set<String> getKeys(String prefix) {
        Iterable<String> keysByPattern = client.getKeys().getKeysByPattern(prefix);
        Set<String> keys = new HashSet<>();
        for (String s : keysByPattern) {
            keys.add(s);
        }
        return keys;
    }

    public void zremrangeByScore(String key, double startScore, double endScore) {
        int removeElementNum = client.getScoredSortedSet(key)
                .removeRangeByScore(startScore, Boolean.TRUE, endScore, Boolean.TRUE);
    }
}
