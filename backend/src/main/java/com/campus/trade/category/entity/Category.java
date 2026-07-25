package com.campus.trade.category.entity;

/**
 * 商品分类数据库实体，对应 category 表。
 *
 * <p>实体只在持久层和业务层内部流转，对外接口使用 CategoryVO，
 * 防止未来增加内部字段后意外暴露给前端。</p>
 */
public class Category {

    private Long id;
    private String name;
    private Integer sort;
    private Integer enabled;

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
