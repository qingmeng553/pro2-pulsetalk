package com.community.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 主页举报 profile_report。 */
@Data
@TableName("profile_report")
public class ProfileReport implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long targetUserId;

    private Long reporterId;

    /** profile=整页，其余为模块类型 */
    private String moduleType;

    private String reason;

    /** 0待处理 / 1已处理 */
    private Integer status;

    private Long handleBy;

    private LocalDateTime handleTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
