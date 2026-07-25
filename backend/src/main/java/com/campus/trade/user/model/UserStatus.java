package com.campus.trade.user.model;

/**
 * 用户账号状态编码。
 */
public enum UserStatus {

    NORMAL(0),
    BANNED(1);

    private final int code;

    UserStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
