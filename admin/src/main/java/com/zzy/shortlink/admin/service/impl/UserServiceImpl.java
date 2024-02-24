package com.zzy.shortlink.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzy.shortlink.admin.common.constant.RedisCacheConstant;
import com.zzy.shortlink.admin.common.convention.exception.ClientException;
import com.zzy.shortlink.admin.common.enums.UserErrorCodeEnum;
import com.zzy.shortlink.admin.dao.entity.UserDO;
import com.zzy.shortlink.admin.dao.mapper.UserMapper;
import com.zzy.shortlink.admin.dto.req.UserLoginReqDTO;
import com.zzy.shortlink.admin.dto.req.UserRegisterReqDTO;
import com.zzy.shortlink.admin.dto.req.UserUpdateReqDTO;
import com.zzy.shortlink.admin.dto.resp.UserLoginRespDTO;
import com.zzy.shortlink.admin.dto.resp.UserRespDTO;
import com.zzy.shortlink.admin.service.GroupService;
import com.zzy.shortlink.admin.service.UserService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDO> implements UserService {

    private final RBloomFilter<String> userRegisterCachePenetrationBloomFilter;
    private final RedissonClient redissonClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final GroupService groupService;

    @Override
    public UserRespDTO getUserByUsername(String username) {
        Boolean isUsernameExist = stringRedisTemplate.hasKey("data_" + username);
        if(isUsernameExist != null && isUsernameExist) {
            String userDO = stringRedisTemplate.opsForValue().get("data_" + username);
            return JSONUtil.toBean(userDO, UserRespDTO.class);
        }
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class).eq(UserDO::getUsername, username);
        UserDO userDO = baseMapper.selectOne(queryWrapper);
        if(userDO == null) {
            throw new ClientException(UserErrorCodeEnum.USER_NULL);
        }
        UserRespDTO result = new UserRespDTO();
        BeanUtils.copyProperties(userDO, result);
        stringRedisTemplate.opsForValue().set("data_" + username, JSON.toJSONString(result), 24L, TimeUnit.HOURS);
        return result;
    }

    @Override
    public Boolean hasUsername(String username) {
        //1. 访问数据库方案
//        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class).eq(UserDO::getUsername, username);
//        UserDO userDO = baseMapper.selectOne(queryWrapper);
//        return userDO == null;
        //2. 布隆过滤器方案
        return userRegisterCachePenetrationBloomFilter.contains(username);
    }

    @Override
    public void register(UserRegisterReqDTO requestParams) {
        if(hasUsername(requestParams.getUsername())) {
           throw new ClientException(UserErrorCodeEnum.USER_NAME_EXIST);
        }
        // 通过分布式锁实现对同一用户名进行大量注册请求的限制
        RLock lock = redissonClient.getLock(RedisCacheConstant.LOCK_USER_REGISTER_KEY + requestParams.getUsername());
        try {
            if(lock.tryLock()) {
                try {
                    int inserted = baseMapper.insert(BeanUtil.toBean(requestParams, UserDO.class));
                    if(inserted < 1) {
                        throw new ClientException(UserErrorCodeEnum.USER_EXIST);
                    }
                } catch (DuplicateKeyException ex) {
                    throw new ClientException(UserErrorCodeEnum.USER_EXIST);
                }
                userRegisterCachePenetrationBloomFilter.add(requestParams.getUsername());
                // 注册用户后自动创建默认分组
                groupService.saveGroup(requestParams.getUsername(), "默认分组");
                return;
            }
            throw new ClientException(UserErrorCodeEnum.USER_NAME_EXIST);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void update(UserUpdateReqDTO requestParams) {
        // TODO 验证当前用户名是否为登录用户
        LambdaQueryWrapper<UserDO> updateWrapper = Wrappers.lambdaQuery(UserDO.class).eq(UserDO::getUsername, requestParams.getUsername());
        baseMapper.update(BeanUtil.toBean(requestParams, UserDO.class), updateWrapper);
        stringRedisTemplate.delete("data_" + requestParams.getUsername());
        // TODO 延迟双删 保证用户信息在缓存和数据库中的一致性
    }

    @Override
    public UserLoginRespDTO login(UserLoginReqDTO requestParams) {
        Boolean hasLogin = stringRedisTemplate.hasKey("login_" + requestParams.getUsername());
        if(hasLogin != null && hasLogin) {
            throw new ClientException(UserErrorCodeEnum.USER_HAS_LOGIN);
        }
        LambdaQueryWrapper<UserDO> selectWrapper = Wrappers.lambdaQuery(UserDO.class).eq(UserDO::getUsername, requestParams.getUsername()).eq(UserDO::getPassword, requestParams.getPassword()).eq(UserDO::getDelFlag, 0);
        UserDO userDO = baseMapper.selectOne(selectWrapper);
        if(userDO == null) {
            throw new ClientException(UserErrorCodeEnum.USER_NAME_NULL);
        }
        String uuid = UUID.randomUUID().toString();
        stringRedisTemplate.opsForHash().put("login_" + requestParams.getUsername(), uuid, JSON.toJSONString(userDO));
        stringRedisTemplate.expire("login_" + requestParams.getUsername(), 30L, TimeUnit.HOURS);
        return new UserLoginRespDTO(uuid);
    }

    @Override
    public Boolean checkLogin(String username, String token) {
        return stringRedisTemplate.opsForHash().get("login_" + username, token) != null;
    }

    @Override
    public void logout(String username, String token) {
        if(checkLogin(username, token)) {
            stringRedisTemplate.delete("login_" + username);
            return;
        }
        throw new ClientException("用户Token不存在或用户未登录");
    }
}
