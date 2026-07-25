package com.campus.trade.user.entity;

import java.time.LocalDateTime;

/**
 * 用户数据库实体，对应 user 表。
 *
 * <p>该对象包含密码哈希，只允许在 Mapper 和 Service 内部使用，
 * Controller 必须返回专门的 VO，不能直接序列化本实体。</p>
 */
public class User {

    /** 数据库自增主键；返回前端时由 Jackson 全局转换为字符串。 */
    private Long id;

    /** 学号，具有唯一索引，也是登录账号之一。 */
    private String studentNo;

    /** 手机号，具有唯一索引，也是登录账号之一。 */
    private String phone;

    /** BCrypt 密码哈希，不是明文密码，禁止通过 VO 返回前端。 */
    private String password;

    /** 用户公开昵称。 */
    private String nickname;

    /** 头像访问地址；新注册用户可以为空。 */
    private String avatar;

    /** 用户所在校区。 */
    private String campus;

    /** 角色编码：0 普通用户，1 管理员。 */
    private Integer role;

    /** 账号状态：0 正常，1 封禁。 */
    private Integer status;

    /** 数据库记录创建时间。 */
    private LocalDateTime createdAt;

    /** 数据库记录最后更新时间。 */
    private LocalDateTime updatedAt;

    /*
     * 下面是标准 JavaBean getter/setter。
     * MyBatis 通过 setter 把查询结果写入实体，Service 通过 getter 读取字段。
     * 这里不使用 Lombok，便于学习阶段直接看到 JavaBean 与 MyBatis 的配合方式。
     */

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStudentNo() {
        return studentNo;
    }

    public void setStudentNo(String studentNo) {
        this.studentNo = studentNo;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getCampus() {
        return campus;
    }

    public void setCampus(String campus) {
        this.campus = campus;
    }

    public Integer getRole() {
        return role;
    }

    public void setRole(Integer role) {
        this.role = role;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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
