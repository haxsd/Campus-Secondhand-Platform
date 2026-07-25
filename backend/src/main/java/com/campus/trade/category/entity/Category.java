package com.campus.trade.category.entity;

/**
 * 商品分类数据库实体，对应 category 表。
 *
 * <p>实体只在持久层和业务层内部流转，对外接口使用 CategoryVO，
 * 防止未来增加内部字段后意外暴露给前端。</p>
 */
public class Category {

    /** 分类主键。 */
    private Long id;

    /** 前端展示的分类名称。 */
    private String name;

    /** 排序值，数值越小越靠前。 */
    private Integer sort;

    /** 是否启用：1 启用，0 停用。 */
    private Integer enabled;

    // MyBatis 通过以下 setter 写入查询结果，再由 Service 通过 getter 读取。

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }
}
