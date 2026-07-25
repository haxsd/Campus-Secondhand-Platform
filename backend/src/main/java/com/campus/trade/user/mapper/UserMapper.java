package com.campus.trade.user.mapper;

import com.campus.trade.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

/**
 * 用户及注册时信用摘要初始化的持久层。
 */
@Mapper
public interface UserMapper {

    /**
     * 按学号或手机号查询登录用户。
     */
    Optional<User> selectByAccount(@Param("account") String account);

    /**
     * 按主键查询用户。
     */
    Optional<User> selectById(@Param("id") Long id);

    /**
     * 注册前快速检查学号或手机号是否已使用。
     *
     * <p>该检查只用于尽早给出友好提示，并发正确性最终仍由数据库唯一索引保证。</p>
     */
    boolean existsByStudentNoOrPhone(
            @Param("studentNo") String studentNo,
            @Param("phone") String phone
    );

    /**
     * 插入用户并回填自增主键到 user.id。
     */
    int insert(User user);

    /**
     * 为新用户初始化信用摘要，必须与用户插入处于同一事务。
     */
    int insertCreditSummary(@Param("userId") Long userId);
}
