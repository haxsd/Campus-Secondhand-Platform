package com.campus.trade.auth.service;

import com.campus.trade.auth.dto.LoginRequest;
import com.campus.trade.auth.dto.RegisterRequest;
import com.campus.trade.auth.jwt.IssuedToken;
import com.campus.trade.auth.jwt.JwtProvider;
import com.campus.trade.auth.vo.CurrentUserVO;
import com.campus.trade.auth.vo.LoginResponse;
import com.campus.trade.auth.vo.LoginUserVO;
import com.campus.trade.common.context.CurrentUser;
import com.campus.trade.common.context.UserContext;
import com.campus.trade.common.exception.BizException;
import com.campus.trade.common.exception.ErrorCode;
import com.campus.trade.user.entity.User;
import com.campus.trade.user.mapper.UserMapper;
import com.campus.trade.user.model.UserRole;
import com.campus.trade.user.model.UserStatus;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 注册、登录、退出及当前用户查询服务。
 */
@Service
public class AuthService {

    private static final String DUPLICATE_USER_MESSAGE = "学号或手机号已注册";
    private static final String LOGIN_FAILED_MESSAGE = "账号或密码错误";

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final LoginSessionService loginSessionService;

    public AuthService(
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            JwtProvider jwtProvider,
            LoginSessionService loginSessionService
    ) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.loginSessionService = loginSessionService;
    }

    /**
     * 注册普通用户，并在同一事务中初始化信用摘要。
     *
     * <p>应用层预查用于友好提示，数据库唯一索引负责并发下的最终正确性。
     * 任一步失败都会回滚，避免出现有用户但没有信用摘要的半成品数据。</p>
     */
    @Transactional
    public void register(RegisterRequest request) {
        String studentNo = request.studentNo().trim();
        String phone = request.phone().trim();

        if (userMapper.existsByStudentNoOrPhone(studentNo, phone)) {
            throw new BizException(ErrorCode.BAD_REQUEST, DUPLICATE_USER_MESSAGE);
        }

        User user = new User();
        user.setStudentNo(studentNo);
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname().trim());
        user.setCampus(request.campus().trim());
        user.setRole(UserRole.USER.getCode());
        user.setStatus(UserStatus.NORMAL.getCode());

        try {
            userMapper.insert(user);
            userMapper.insertCreditSummary(user.getId());
        } catch (DuplicateKeyException exception) {
            // 两个并发注册可能同时通过预查，唯一索引冲突仍统一转成契约中的 400。
            throw new BizException(ErrorCode.BAD_REQUEST, DUPLICATE_USER_MESSAGE);
        }
    }

    /**
     * 校验账号密码，签发 JWT，并写入 Redis 登录态。
     */
    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectByAccount(request.account().trim())
                .filter(found -> passwordEncoder.matches(request.password(), found.getPassword()))
                .orElseThrow(() -> new BizException(ErrorCode.BAD_REQUEST, LOGIN_FAILED_MESSAGE));

        if (UserStatus.BANNED.getCode() == user.getStatus()) {
            throw new BizException(ErrorCode.FORBIDDEN, "账号已被封禁");
        }

        IssuedToken issuedToken = jwtProvider.issue(user.getId(), user.getRole());
        loginSessionService.create(
                issuedToken.tokenId(),
                user.getId(),
                user.getRole(),
                issuedToken.ttl()
        );

        LoginUserVO loginUser = new LoginUserVO(
                user.getId(),
                user.getNickname(),
                user.getAvatar(),
                user.getCampus(),
                user.getRole()
        );
        return new LoginResponse(issuedToken.value(), loginUser);
    }

    /**
     * 立即删除当前 token 的 Redis 白名单记录。
     */
    public void logout() {
        CurrentUser currentUser = UserContext.requireCurrentUser();
        loginSessionService.delete(currentUser.tokenId());
    }

    /**
     * 从数据库读取当前用户，避免返回 token 中可能已经过时的昵称等资料。
     */
    @Transactional(readOnly = true)
    public CurrentUserVO currentUser() {
        Long userId = UserContext.requireCurrentUser().userId();
        User user = userMapper.selectById(userId)
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED, "登录用户不存在"));

        return new CurrentUserVO(
                user.getId(),
                user.getStudentNo(),
                user.getNickname(),
                user.getAvatar(),
                user.getCampus(),
                user.getRole()
        );
    }
}
