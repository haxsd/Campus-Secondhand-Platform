package com.campus.trade.product.model;

import java.util.Arrays;

/**
 * 商品状态机的全部状态，与 product.status 数据库字段及前端常量保持一致。
 *
 * <p>状态不能由前端随意指定。Service 会根据当前状态决定允许的下一步操作，
 * 例如草稿可以申请审核，但待审核商品只能撤回，不能继续编辑。</p>
 */
public enum ProductStatus {

    DRAFT(0),
    PENDING_REVIEW(1),
    REJECTED(2),
    ON_SALE(3),
    OFF_SHELF(4),
    SOLD_OUT(5),
    AI_REVIEWING(6);

    private final int code;

    ProductStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    /**
     * 判断一个外部输入的数字是否是项目认可的商品状态编码。
     */
    public static boolean isValid(Integer code) {
        return code != null && Arrays.stream(values()).anyMatch(status -> status.code == code);
    }
}
