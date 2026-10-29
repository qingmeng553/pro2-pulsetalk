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

/** 主页留言板 user_guestbook。 */
@Data
@TableName("user_guestbook")
public class UserGuestbook implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 主页主人 */
    private Long ownerId;

    /** 留言人 */
    private Long fromUserId;

    /** 留言内容 ≤200 字 */
    private String content;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
