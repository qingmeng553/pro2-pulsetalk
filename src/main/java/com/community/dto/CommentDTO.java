package com.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发表评论请求（一级评论）
 */
@Data
public class CommentDTO {

    /** 评论内容 */
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 2000, message = "评论最长2000个字符")
    private String content;
}
