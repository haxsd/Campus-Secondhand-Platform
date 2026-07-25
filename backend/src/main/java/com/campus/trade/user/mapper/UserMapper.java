package com.campus.trade.user.mapper;

import com.campus.trade.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

/**
 * 用户及注册时信用摘要初始化的持久层。
 *
 * <p>Mapper 接口只声明数据库操作，不写业务判断。具体 SQL 位于
 * {@code resources/mapper/user/UserMapper.xml}，方法名和 XML 中的 id 必须完全一致。</p>
 */
@Mapper
public interface UserMapper {

    /**
     * 按学号或手机号查询登录用户。
     *
     * <p>使用 Optional 表达“账号可能不存在”，调用方无需处理 null。</p>
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
     *
     * @return 受影响行数，正常为 1
     */
    int insert(User user);

    /**
     * 为新用户初始化信用摘要，必须与用户插入处于同一事务。
     *
     * @return 受影响行数，正常为 1
     */
    int insertCreditSummary(@Param("userId") Long userId);
}
