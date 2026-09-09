package com.community.vo;

import lombok.Data;

/**
 * 登录结果 VO：返回 sa-token 及用户信息
 */
@Data
public class LoginVO {

    /** token 名称(请求头携带键，即 sa-token 配置的 token-name: satoken) */
    private String tokenName;

    /** token 值 */
    private String tokenValue;

    /** 登录用户信息(含 role) */
    private CurrentUserVO user;
}
