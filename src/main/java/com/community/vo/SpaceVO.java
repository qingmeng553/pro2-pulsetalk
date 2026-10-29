package com.community.vo;

import com.community.entity.UserRole;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 可配置主页：用户摘要 + 布局 + 统计 + 被隐藏模块。 */
@Data
public class SpaceVO {

    private Long userId;

    private String nickname;

    private String avatarUrl;

    private UserRole role;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    private JsonNode layout;

    private Integer version;

    /** 管理员隐藏的模块类型（前端过滤） */
    private List<String> hiddenModules;

    private SpaceStatsVO stats;
}
