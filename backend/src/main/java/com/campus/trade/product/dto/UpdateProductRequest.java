package com.campus.trade.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 编辑商品的请求参数。
 *
 * <p>除商品内容外必须携带 version。后端会把它作为乐观锁条件，
 * 防止两个页面先后覆盖彼此的修改。</p>
 */
public record UpdateProductRequest(
        @NotBlank(message = "请输入商品标题")
        @Size(max = 60, message = "商品标题不能超过 60 个字符")
        String title,

        @NotBlank(message = "请输入商品描述")
        @Size(max = 2000, message = "商品描述不能超过 2000 个字符")
        String description,

        @NotNull(message = "请输入商品价格")
        @DecimalMin(value = "0.01", message = "商品价格必须大于 0")
        @Digits(integer = 8, fraction = 2, message = "商品价格最多 8 位整数和 2 位小数")
        BigDecimal price,

        @NotNull(message = "请输入库存")
        @Min(value = 1, message = "库存至少为 1")
        @Max(value = 99999, message = "库存不能超过 99999")
        Integer stock,

        @NotNull(message = "请选择商品成色")
        Integer itemCondition,

        @NotNull(message = "请选择商品分类")
        Long categoryId,

        @NotBlank(message = "请选择校区")
        @Size(max = 30, message = "校区名称不能超过 30 个字符")
        String campus,

        @NotBlank(message = "请填写交易地点")
        @Size(max = 60, message = "交易地点不能超过 60 个字符")
        String tradePlace,

        @NotEmpty(message = "请至少上传 1 张商品图片")
        @Size(max = 5, message = "商品图片最多 5 张")
        List<@NotBlank(message = "图片地址不能为空") @Size(max = 255, message = "图片地址过长") String> images,

        @NotNull(message = "商品版本不能为空，请刷新后重试")
        @Min(value = 0, message = "商品版本不正确")
        Integer version
) {
}
