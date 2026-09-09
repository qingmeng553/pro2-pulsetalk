package com.community.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体 sys_user
 */
@Data
@TableName("sys_user")
public class SysUser implements Serializable {

    /** 主键(数据库自增) */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录账号(唯一) */
    private String username;

    /** 密码(加密存储，格式 salt$sha256hex，见 PasswordUtil) */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 头像URL(MinIO 对象存储地址) */
    private String avatarUrl;

    /** 角色 USER / ADMIN */
    private UserRole role;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
