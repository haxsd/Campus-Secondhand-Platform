package com.campus.trade.auth.vo;

/**
 * 登录成功响应数据。
 *
 * @param token JWT 字符串，后续请求放入 Authorization: Bearer 请求头
 * @param user  当前用户的安全公开信息
 */
public record LoginResponse(String token, LoginUserVO user) {
}
