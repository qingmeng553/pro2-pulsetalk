package com.community.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 管理员隐藏的主页模块 profile_hidden_module。 */
@Data
@TableName("profile_hidden_module")
public class ProfileHiddenModule implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String moduleType;

    private Long hiddenBy;

    private String reason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
