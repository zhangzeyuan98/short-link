package com.zzy.shortlink.project.config;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(RedissonClient.class)
@RequiredArgsConstructor
public class LinkRateLimiter {

    private final RedissonClient redissonClient;

    private static final String LINK_CREATE_RATE_LIMITER_PREFIX = "link_create_interface_flow_risk_control";

    @Bean
    public RRateLimiter linkCreateRateLimiter() {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(LINK_CREATE_RATE_LIMITER_PREFIX);
        rateLimiter.setRateAsync(RateType.OVERALL, 3, 5, RateIntervalUnit.SECONDS);
        return rateLimiter;
    }
}
