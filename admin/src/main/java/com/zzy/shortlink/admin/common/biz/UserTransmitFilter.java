package com.zzy.shortlink.admin.common.biz;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.zzy.shortlink.admin.common.convention.exception.ClientException;
import com.zzy.shortlink.admin.common.enums.UserErrorCodeEnum;
import com.zzy.shortlink.admin.dto.req.GroupSaveReqDTO;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.assertj.core.util.Lists;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * 用户信息传输过滤
 */
@RequiredArgsConstructor
public class UserTransmitFilter implements Filter {

    private final StringRedisTemplate stringRedisTemplate;

    private static final List<String> IGNORE_URI = Lists.newArrayList(
            "/api/short-link/v1/user/login",
            "/api/short-link/v1/user/has-username",
            "/api/short-link/admin/v1/page",
            "/api/short-link/admin/v1/create",
            "/api/short-link/v1/user/check-login"
    );

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        String requestURI = httpServletRequest.getRequestURI();
        if(!IGNORE_URI.contains(requestURI)) {
            String method = httpServletRequest.getMethod();
            if(!(Objects.equals(requestURI, "/api/short-link/v1/user/") && Objects.equals(method, "POST"))) {
                String username = httpServletRequest.getHeader("username");
                String token = httpServletRequest.getHeader("token");
                if(!StrUtil.isAllNotBlank(username, token)) {
                    // TODO 全局异常拦截器是拦截不到过滤器这一层级的, 因为请求是先经过过滤器再经过拦截器的
                    throw new ClientException(UserErrorCodeEnum.USER_TOKEN_FAIL);
                }
                Object userInfoJsonStr = null;
                try {
                    userInfoJsonStr = stringRedisTemplate.opsForHash().get("login_" + username, token);
                    if(userInfoJsonStr == null) {
                        throw new ClientException(UserErrorCodeEnum.USER_TOKEN_FAIL);
                    }
                } catch (Exception ex) {
                    throw new ClientException(UserErrorCodeEnum.USER_TOKEN_FAIL);
                }
                UserInfoDTO userInfoDTO = JSON.parseObject(userInfoJsonStr.toString(), UserInfoDTO.class);
                UserContext.setUser(userInfoDTO);
            }
        }
        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            UserContext.removeUser();
        }
    }
}
