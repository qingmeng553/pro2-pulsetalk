package com.community.common.util;

import com.community.entity.SysUser;
import com.community.vo.AuthorVO;

/**
 * 作者摘要装配工具(昵称+头像+角色，用于帖子卡片/评论/通知/会话/好友等)
 */
public final class AuthorVOs {

    private AuthorVOs() {
    }

    /**
     * 用户实体 → 作者摘要；用户缺失(理论上不会)时兜底为“已注销用户”
     */
    public static AuthorVO of(SysUser user) {
        AuthorVO vo = new AuthorVO();
        if (user == null) {
            vo.setNickname("已注销用户");
            return vo;
        }
        vo.setUserId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setRole(user.getRole());
        return vo;
    }
}
