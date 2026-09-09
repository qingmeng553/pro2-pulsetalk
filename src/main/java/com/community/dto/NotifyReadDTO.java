package com.community.dto;

import lombok.Data;

import java.util.List;

/**
 * 通知已读请求(ids 为空表示全部标记已读)
 */
@Data
public class NotifyReadDTO {

    /** 通知ID列表；为空则将该用户全部通知标记已读 */
    private List<Long> ids;
}
