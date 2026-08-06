package com.campus.trade.user.mapper;

import com.campus.trade.user.entity.CreditSummary;
import com.campus.trade.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.Optional;

@Mapper
public interface UserMapper {
    Optional<User> selectByAccount(@Param("account") String account);
    Optional<User> selectById(@Param("id") Long id);
    boolean existsByStudentNoOrPhone(@Param("studentNo") String studentNo, @Param("phone") String phone);
    int insert(User user);
    int insertCreditSummary(@Param("userId") Long userId);
    int incrementDealCount(@Param("userId") Long userId);
    int updateProfile(User user);
    int updatePassword(@Param("userId") Long userId, @Param("password") String password);
    Optional<CreditSummary> selectCreditSummary(@Param("userId") Long userId);
}
