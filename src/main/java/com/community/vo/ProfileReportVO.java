package com.community.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/** 管理端举报条目。 */
@Data
public class ProfileReportVO {

    private Long id;

    private Long targetUserId;

    private String targetNickname;

    private Long reporterId;

    private String reporterNickname;

    private String moduleType;

    private String reason;

    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime handleTime;
}
