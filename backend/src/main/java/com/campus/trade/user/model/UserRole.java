package com.campus.trade.user.model;

/**
 * 用户角色编码，与数据库及前端 constants 保持一致。
 */
public enum UserRole {

    USER(0),
    ADMIN(1);

    private final int code;

    UserRole(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
