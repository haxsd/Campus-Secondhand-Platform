package com.campus.trade.user.model;

/**
 * 用户角色编码，与数据库及前端 constants 保持一致。
 *
 * <p>使用枚举代替代码中的魔法数字。例如用 {@code UserRole.ADMIN.getCode()}
 * 比直接写 {@code 1} 更容易理解，也减少三方编码不一致的风险。</p>
 */
public enum UserRole {

    USER(0),
    ADMIN(1);

    private final int code;

    UserRole(int code) {
        this.code = code;
    }

    public int getCode() {
        // 数据库存储的是稳定数字编码，而不是可能变化的中文名称。
        return code;
    }
}
