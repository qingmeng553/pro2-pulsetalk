package com.community.vo;

import lombok.Data;

/**
 * 点赞/收藏切换结果 VO
 */
@Data
public class InteractionVO {

    /** 切换后的状态：true=已点赞/已收藏 */
    private Boolean active;

    /** 最新计数 */
    private Long count;
}
