package com.campus.trade.product.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品主表实体，对应 product 表。
 *
 * <p>该实体代表可持久化的商品数据。商品图片在 product_image 表中单独保存，
 * 因此 images 不放在本实体中；对前端返回时由 Service 组合为专用 VO。</p>
 */
public class Product {

    /** 商品自增主键。 */
    private Long id;

    /** 发布者 ID，只能从 UserContext 获取，绝不能信任前端传入。 */
    private Long sellerId;

    /** 商品分类 ID。 */
    private Long categoryId;

    /** 商品标题，数据库最大 60 个字符。 */
    private String title;

    /** 商品文字描述，数据库最大 2000 个字符。 */
    private String description;

    /** 金额使用 BigDecimal，避免 double 的二进制精度误差。 */
    private BigDecimal price;

    /** 当前可售库存。 */
    private Integer stock;

    /** 商品成色编码，取值见 ItemCondition。 */
    private Integer itemCondition;

    /** 商品所在校区。 */
    private String campus;

    /** 默认线下交易地点。 */
    private String tradePlace;

    /** 商品状态，取值见 ProductStatus。 */
    private Integer status;

    /** 商品详情被浏览的次数；当前阶段只读取，异步累加后续实现。 */
    private Integer viewCount;

    /** 编辑乐观锁版本号。 */
    private Integer version;

    /** 软删除标记：0 正常，1 删除。 */
    private Integer deleted;

    /** 创建时间。 */
    private LocalDateTime createdAt;

    /** 最近更新时间。 */
    private LocalDateTime updatedAt;

    // 以下是 MyBatis 映射查询结果与 Service 组装数据所需的标准 JavaBean 方法。

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Integer getItemCondition() {
        return itemCondition;
    }

    public void setItemCondition(Integer itemCondition) {
        this.itemCondition = itemCondition;
    }

    public String getCampus() {
        return campus;
    }

    public void setCampus(String campus) {
        this.campus = campus;
    }

    public String getTradePlace() {
        return tradePlace;
    }

    public void setTradePlace(String tradePlace) {
        this.tradePlace = tradePlace;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
