package com.campus.trade.user.model;

/**
 * 用户账号状态编码。
 *
 * <p>状态含义必须与 user.status 字段及 API 文档保持一致。</p>
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
