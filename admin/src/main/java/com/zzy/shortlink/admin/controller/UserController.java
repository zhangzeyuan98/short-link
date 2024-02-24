package com.zzy.shortlink.admin.controller;


import cn.hutool.core.bean.BeanUtil;
import com.zzy.shortlink.admin.common.convention.exception.ClientException;
import com.zzy.shortlink.admin.common.convention.result.Result;
import com.zzy.shortlink.admin.common.convention.result.Results;
import com.zzy.shortlink.admin.common.enums.UserErrorCodeEnum;
import com.zzy.shortlink.admin.dto.req.UserLoginReqDTO;
import com.zzy.shortlink.admin.dto.req.UserRegisterReqDTO;
import com.zzy.shortlink.admin.dto.req.UserUpdateReqDTO;
import com.zzy.shortlink.admin.dto.resp.UserActualRespDTO;
import com.zzy.shortlink.admin.dto.resp.UserLoginRespDTO;
import com.zzy.shortlink.admin.dto.resp.UserRespDTO;
import com.zzy.shortlink.admin.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/api/short-link/v1/user/{username}")
    public Result<UserRespDTO> getUserByUsername(@PathVariable("username") String username) {
        UserRespDTO result = userService.getUserByUsername(username);
        if(result == null)
            throw new ClientException(UserErrorCodeEnum.USER_NULL);
        return Results.success(result);
    }

    @GetMapping("/api/short-link/v1/actual/user/{username}")
    public Result<UserActualRespDTO> getActualUserByUsername(@PathVariable("username") String username) {
        UserRespDTO result = userService.getUserByUsername(username);
        if(result == null)
            throw new ClientException(UserErrorCodeEnum.USER_NULL);
        return Results.success(BeanUtil.toBean(result, UserActualRespDTO.class));
    }

    @GetMapping("/api/short-link/v1/user/has-username/{username}")
    public Result<Boolean> hasUsername(@PathVariable("username") String username) {
        return Results.success(userService.hasUsername(username));
    }

    @PostMapping("/api/short-link/v1/user/")
    public Result<Void> register(@RequestBody UserRegisterReqDTO requestParams) {
        userService.register(requestParams);
        return Results.success();
    }

    @PutMapping("/api/short-link/v1/user/")
    public Result<Void> update(@RequestBody UserUpdateReqDTO requestParams) {
        userService.update(requestParams);
        return Results.success();
    }

    @PostMapping("/api/short-link/v1/user/login")
    public Result<UserLoginRespDTO> login(@RequestBody UserLoginReqDTO requestParams) {
        UserLoginRespDTO userLoginRespDTO = userService.login(requestParams);
        return Results.success(userLoginRespDTO);
    }

    @GetMapping("/api/short-link/v1/user/check-login")
    public Result<Boolean> checkLogin(@RequestParam("username") String username, @RequestParam("token") String token) {
        return Results.success(userService.checkLogin(username, token));
    }
    @DeleteMapping("/api/short-link/v1/user/logout")
    public Result<Void> logout(@RequestParam("username") String username, @RequestParam("token") String token) {
        userService.logout(username, token);
        return Results.success();
    }
}
