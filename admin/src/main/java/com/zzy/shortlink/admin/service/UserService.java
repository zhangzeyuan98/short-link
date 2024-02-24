package com.zzy.shortlink.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zzy.shortlink.admin.dao.entity.UserDO;
import com.zzy.shortlink.admin.dto.req.UserLoginReqDTO;
import com.zzy.shortlink.admin.dto.req.UserRegisterReqDTO;
import com.zzy.shortlink.admin.dto.req.UserUpdateReqDTO;
import com.zzy.shortlink.admin.dto.resp.UserLoginRespDTO;
import com.zzy.shortlink.admin.dto.resp.UserRespDTO;

public interface UserService extends IService<UserDO> {
    UserRespDTO getUserByUsername(String username);

    Boolean hasUsername(String username);

    void register(UserRegisterReqDTO requestParams);

    void update(UserUpdateReqDTO requestParams);

    UserLoginRespDTO login(UserLoginReqDTO requestParams);

    Boolean checkLogin(String username, String token);

    void logout(String username, String token);
}
