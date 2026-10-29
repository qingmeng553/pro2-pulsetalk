package com.community.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 用户实体 sys_user。 */
@Data
@TableName("sys_user")
public class SysUser implements Serializable {

    /** 账号状态：正常 */
    public static final int STATUS_NORMAL = 0;
    /** 账号状态：封禁 */
    public static final int STATUS_BANNED = 1;

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

    /** 绑定邮箱(唯一，可为空；alpha2) */
    private String email;

    /** 邮箱是否已验证 0未验证 / 1已验证 */
    private Integer emailVerified;

    /** 密保问题(v3)：由用户自行设置 */
    private String securityQuestion;

    /** 密保答案(加密存储，v3) */
    private String securityAnswer;

    /** 角色 USER / ADMIN */
    private UserRole role;

    /** 账号状态 0正常 / 1封禁 (v3) */
    private Integer status;

    /** 逻辑删除 0未删 / 1已删 (v3，管理员删除用户) */
    @TableLogic
    private Integer isDeleted;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
