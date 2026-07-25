package com.campus.trade.user.service;

import com.campus.trade.auth.service.LoginSessionService;
import com.campus.trade.common.context.CurrentUser;
import com.campus.trade.common.context.UserContext;
import com.campus.trade.common.exception.BizException;
import com.campus.trade.common.exception.ErrorCode;
import com.campus.trade.user.dto.ChangePasswordRequest;
import com.campus.trade.user.dto.UpdateProfileRequest;
import com.campus.trade.user.entity.User;
import com.campus.trade.user.mapper.UserMapper;
import com.campus.trade.user.vo.UserProfileVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

/**
 * 个人中心业务服务。
 *
 * <p>此类只处理“当前登录用户自己的资料”。用户 ID 一律从经过 JWT 验签的
 * {@link UserContext} 获取，不接收前端传来的 userId，以避免越权读取或修改他人资料。</p>
 */
@Service
public class UserProfileService {

    /**
     * FileStorageService 生成的头像 URL 格式：/api/uploads/{UUID}.{jpg|png|webp}。
     * 只接受这一受控格式，避免将任意外链或不规范的内部路径保存为头像。
     */
    private static final Pattern UPLOAD_URL_PATTERN = Pattern.compile(
            "^/api/uploads/[0-9a-fA-F-]{36}\\.(?:jpg|png|webp)$"
    );

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final LoginSessionService loginSessionService;

    public UserProfileService(
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            LoginSessionService loginSessionService
    ) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.loginSessionService = loginSessionService;
    }

    /**
     * 查询当前用户完整的个人中心资料。
     *
     * <p>不能复用登录响应：个人中心需要展示手机号和学号，而登录响应只应携带顶栏所需的最小字段。</p>
     */
    @Transactional(readOnly = true)
    public UserProfileVO getCurrentProfile() {
        return toProfileVO(findCurrentUser());
    }

    /**
     * 更新昵称、校区和头像。
     *
     * <p>客户端只能传入 DTO 中定义的三个显示字段；Mapper 的更新 SQL 同样不会更新任何身份、安全字段，
     * 两层限制共同防止误修改学号、手机号、角色或账号状态。</p>
     */
    @Transactional
    public UserProfileVO updateCurrentProfile(UpdateProfileRequest request) {
        User user = findCurrentUser();
        user.setNickname(request.nickname().trim());
        user.setCampus(request.campus().trim());
        user.setAvatar(normalizeAvatar(request.avatar()));

        if (userMapper.updateProfile(user) != 1) {
            // 正常情况下 user 已由 selectById 查出，更新一定命中一行；保留判断便于识别并发删除等异常情况。
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在或已被删除");
        }
        return toProfileVO(user);
    }

    /**
     * 校验旧密码后更新密码，并吊销当前登录令牌。
     *
     * <p>密码成功变更后，浏览器必须重新使用新密码登录；删除 Redis 中当前 token 的白名单记录，
     * 可以使旧密码仍登录着的本浏览器会话立即失效。</p>
     */
    @Transactional
    public void changeCurrentPassword(ChangePasswordRequest request) {
        CurrentUser currentUser = UserContext.requireCurrentUser();
        User user = findUserById(currentUser.userId());

        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "当前密码错误");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "新密码不能与当前密码相同");
        }
        if (userMapper.updatePassword(user.getId(), passwordEncoder.encode(request.newPassword())) != 1) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在或已被删除");
        }

        // 只吊销本次请求使用的 token；其他设备会话的统一吊销需要额外维护“用户-令牌索引”，当前版本未建立该索引。
        loginSessionService.delete(currentUser.tokenId());
    }

    /** 从认证上下文中取得用户 ID，再查询最新数据库记录。 */
    private User findCurrentUser() {
        return findUserById(UserContext.requireCurrentUser().userId());
    }

    private User findUserById(Long userId) {
        return userMapper.selectById(userId)
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED, "登录用户不存在"));
    }

    /**
     * 统一处理头像的“未设置”语义并校验图片来源。
     *
     * <p>头像 URL 不接受第三方地址或任意内部路径，避免资料页成为外链追踪图片的载体；
     * 用户必须先经 /files/upload 上传，再保存该接口返回的 URL。</p>
     */
    private String normalizeAvatar(String avatar) {
        if (avatar == null || avatar.isBlank()) {
            return null;
        }
        String normalizedAvatar = avatar.trim();
        if (!UPLOAD_URL_PATTERN.matcher(normalizedAvatar).matches()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "头像必须使用本平台上传的图片");
        }
        return normalizedAvatar;
    }

    /** 把数据库实体转换为不会暴露密码哈希的个人中心响应。 */
    private UserProfileVO toProfileVO(User user) {
        return new UserProfileVO(
                user.getId(),
                user.getStudentNo(),
                user.getPhone(),
                user.getNickname(),
                user.getAvatar(),
                user.getCampus(),
                user.getRole()
        );
    }
}
