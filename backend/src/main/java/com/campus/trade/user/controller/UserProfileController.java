package com.campus.trade.user.controller;

import com.campus.trade.common.response.Result;
import com.campus.trade.user.dto.ChangePasswordRequest;
import com.campus.trade.user.dto.UpdateProfileRequest;
import com.campus.trade.user.service.UserProfileService;
import com.campus.trade.user.vo.UserProfileVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前用户个人中心接口。
 *
 * <p>所有 /users/me 请求均需要登录。路径中的 me 不是客户端传入的 ID，
 * Service 会从 JWT 认证上下文中取得真实用户 ID，因此用户只能管理自己的资料。</p>
 */
@RestController
@RequestMapping("/users/me")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    /** 获取个人中心完整资料。 */
    @GetMapping
    public Result<UserProfileVO> getCurrentProfile() {
        return Result.ok(userProfileService.getCurrentProfile());
    }

    /** 更新昵称、校区和头像。 */
    @PutMapping
    public Result<UserProfileVO> updateCurrentProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return Result.ok(userProfileService.updateCurrentProfile(request));
    }

    /** 修改密码；成功后当前登录令牌会失效，前端应清除本地登录态并跳转登录页。 */
    @PutMapping("/password")
    public Result<Void> changeCurrentPassword(@Valid @RequestBody ChangePasswordRequest request) {
        userProfileService.changeCurrentPassword(request);
        return Result.ok();
    }
}
