package com.zzy.shortlink.admin.common.enums;

import com.zzy.shortlink.admin.common.convention.errorcode.IErrorCode;

public enum UserErrorCodeEnum implements IErrorCode {

    USER_TOKEN_FAIL("A000200", "用户TOKEN验证失败"),

    USER_NULL("B000200", "用户记录不存在"),
    USER_NAME_EXIST("B000201", "用户名已存在"),
    USER_NAME_NULL("B000202", "用户名不存在"),
    USER_EXIST("B000203", "用户记录已存在"),

    USER_SAVE_ERROR("B000204", "用户记录新增失败"),

    USER_HAS_LOGIN("B000205", "用户已经登录");

    private String code;

    private String message;

    UserErrorCodeEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
