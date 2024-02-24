package com.zzy.shortlink.project.toolkit;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class FunnelRateLimiterUtil {
    static class Funnel {
        int capacity;
        float leakingRate;
        int leftQuota;
        long leakingTs;
        public Funnel(int capacity, float leakingRate) {
            // 漏斗容量
            this.capacity = capacity;
            // 漏水速率
            this.leakingRate = leakingRate;
            // 剩余空间
            this.leftQuota = capacity;
            // 上一次漏水时间
            this.leakingTs = System.currentTimeMillis();
        }

        void makeSpace() {
            long nowTs = System.currentTimeMillis();
            // 距离上次漏水过去的时间
            long deltaTs = nowTs - leakingTs;
            // 这期间可以腾出的漏斗空间
            int deltaQuota = (int) (deltaTs * leakingRate);
            // 间隔时间太长，整数数字过大溢出，（业务上相当于漏斗清空）
            if (deltaQuota < 0) {
                this.leftQuota = capacity;
                this.leakingTs = nowTs;
                return;
            }
            // 腾出空间太小，最小单位是 1，即一个行为最起码占据1个空间
            if (deltaQuota < 1) {
                return;
            }
            // 增加剩余空间
            this.leftQuota += deltaQuota;
            // 记录漏水时间
            this.leakingTs = nowTs;
            if (this.leftQuota > this.capacity) {
                this.leftQuota = this.capacity;
            }
        }
        boolean watering(int quota) {
            makeSpace();
            // 判断剩余容量够不够
            if (this.leftQuota >= quota) {
                this.leftQuota -= quota;
                return true;
            }
            return false;
        }
    }
    private final Map<String, Funnel> funnels = new HashMap<>();
    public boolean isActionAllowed(String userId, String actionKey, int capacity, float leakingRate) {
        String key = String.format("%s:%s", userId, actionKey);
        Funnel funnel = funnels.get(key);
        if (funnel == null) {
            funnel = new Funnel(capacity, leakingRate);
            funnels.put(key, funnel);
        }
        // 一个行为对应需要 1 个 quota（空间）
        return funnel.watering(1);
    }
}

