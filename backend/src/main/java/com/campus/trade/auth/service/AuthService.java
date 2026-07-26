package com.campus.trade.auth.service;

import com.campus.trade.auth.dto.LoginRequest;
import com.campus.trade.auth.dto.RegisterRequest;
import com.campus.trade.auth.jwt.IssuedToken;
import com.campus.trade.auth.jwt.JwtProvider;
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
 *
 * <p>Service 是认证模块的业务核心。Controller 不直接访问 Mapper，
 * 而是统一经过本类，以便把事务、密码处理和登录态操作放在一个清晰的业务边界内。</p>
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
        // 以下依赖都通过构造器注入，测试时可以方便地替换为 Mock 对象。
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
        // 第 1 步：去除账号两端的空格，避免“20260001”和“ 20260001 ”成为两个账号。
        // 密码不能 trim，因为空格可能是用户主动设置的密码内容。
        String studentNo = request.studentNo().trim();
        String phone = request.phone().trim();

        // 第 2 步：注册前先查询一次，主要目的是尽早返回容易理解的错误信息。
        // 这一步不能替代数据库唯一索引，因为并发请求可能同时查询到“不存在”。
        if (userMapper.existsByStudentNoOrPhone(studentNo, phone)) {
            throw new BizException(ErrorCode.BAD_REQUEST, DUPLICATE_USER_MESSAGE);
        }

        // 第 3 步：把 DTO 转换为数据库实体。
        // sellerId、role、status 等安全字段不能由前端决定，必须由后端赋值。
        User user = new User();
        user.setStudentNo(studentNo);
        user.setPhone(phone);

        // BCrypt 会自动生成随机盐，数据库只保存哈希，绝不保存明文密码。
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname().trim());
        user.setCampus(request.campus().trim());
        user.setRole(UserRole.USER.getCode());
        user.setStatus(UserStatus.NORMAL.getCode());

        try {
            // 第 4 步：插入用户。MyBatis 的 useGeneratedKeys 会把自增 ID 回填到 user.id。
            userMapper.insert(user);

            // 第 5 步：为新用户初始化信用摘要。
            // 两次插入位于同一个 @Transactional 方法中，任意一步失败都会整体回滚。
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
        // 第 1 步：同一条 SQL 同时支持“学号登录”和“手机号登录”。
        User user = userMapper.selectByAccount(request.account().trim())
                // 第 2 步：用 BCrypt 比较明文密码与数据库哈希。
                // 账号不存在和密码错误使用同一个提示，避免攻击者探测哪些账号真实存在。
                .filter(found -> passwordEncoder.matches(request.password(), found.getPassword()))
                .orElseThrow(() -> new BizException(ErrorCode.BAD_REQUEST, LOGIN_FAILED_MESSAGE));

        // 第 3 步：密码正确后再判断账号状态；封禁账号不能获得新 token。
        if (UserStatus.BANNED.getCode() == user.getStatus()) {
            throw new BizException(ErrorCode.FORBIDDEN, "账号已被封禁");
        }

        // 第 4 步：签发 JWT。JWT 中只保存身份字段，不放昵称、手机号或密码等资料。
        IssuedToken issuedToken = jwtProvider.issue(user.getId(), user.getRole());

        // 第 5 步：把 JWT 的 jti 写入 Redis 白名单。
        // 后续请求必须同时通过 JWT 验签和 Redis 检查，退出后才能立即失效。
        loginSessionService.create(
                issuedToken.tokenId(),
                issuedToken.ttl()
        );

        // 第 6 步：组装安全的登录响应。
        // 注意不能返回 User 实体，否则 password 哈希也可能被序列化给前端。
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
        // LoginInterceptor 已经完成 JWT 和 Redis 校验，因此这里可以安全取得当前用户。
        CurrentUser currentUser = UserContext.requireCurrentUser();

        // 只删除当前设备/当前 token 的登录态，不影响该用户在其他设备上的登录。
        loginSessionService.delete(currentUser.tokenId());
    }

}
