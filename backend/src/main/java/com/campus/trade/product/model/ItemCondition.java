package com.campus.trade.product.model;

import java.util.Arrays;

/**
 * 商品成色编码。
 *
 * <p>数据库只保存稳定的数字编码，中文展示文案由前端 constants 负责。
 * 这样后端不需要因为文案变化修改历史数据。</p>
 */
public enum ItemCondition {

    BRAND_NEW(0),
    LIKE_NEW(1),
    LIGHTLY_USED(2),
    OBVIOUSLY_USED(3);

    private final int code;

    ItemCondition(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    /**
     * 校验前端提交的成色是否属于当前枚举范围。
     */
    public static boolean isValid(Integer code) {
        return code != null && Arrays.stream(values()).anyMatch(condition -> condition.code == code);
    }
}
