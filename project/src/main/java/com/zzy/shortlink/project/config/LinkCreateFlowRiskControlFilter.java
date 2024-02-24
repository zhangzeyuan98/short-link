package com.zzy.shortlink.project.config;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.zzy.shortlink.project.common.convention.exception.ClientException;
import com.zzy.shortlink.project.common.convention.exception.ServiceException;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.assertj.core.util.Lists;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 流量风控，请求过滤
 */
@RequiredArgsConstructor
public class LinkCreateFlowRiskControlFilter implements Filter {

    private final StringRedisTemplate stringRedisTemplate;

    private final RedissonClient redissonClient;

    private static final List<String> FORCE_URI = Lists.newArrayList(
            "/api/short-link/v1/create"
    );

    private static final String LINK_CREATE_INTERFACE_FLOW_RISK_CONTROL = "lua/link_create_interface_flow_risk_control.lua";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
//        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
//        String requestURI = httpServletRequest.getRequestURI();
//        if(FORCE_URI.contains(requestURI)) {
//            String method = httpServletRequest.getMethod();
//            String username = httpServletRequest.getHeader("username");
//            long now = DateUtil.date().getTime();
//            DefaultRedisScript<Boolean> redisScript = new DefaultRedisScript<>();
//            redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource(LINK_CREATE_INTERFACE_FLOW_RISK_CONTROL)));
//            redisScript.setResultType(Boolean.class);
//            Boolean execute = stringRedisTemplate.execute(redisScript, Arrays.asList("111", "2222"), now);
//            if(execute == null || Objects.equals(execute, Boolean.FALSE)) {
//                throw new ServiceException("请求频率过高，请求失败!!!!");
//            }
//        }
        filterChain.doFilter(servletRequest, servletResponse);
    }
}
