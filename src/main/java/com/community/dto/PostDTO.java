package com.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 新建/编辑帖子请求（Markdown 正文 + emoji + 多张配图）
 */
@Data
public class PostDTO {

    /** 标题 */
    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题最长100个字符")
    private String title;

    /** Markdown 正文 */
    @NotBlank(message = "正文不能为空")
    @Size(max = 20000, message = "正文最长20000个字符")
    private String content;

    /** 分类ID */
    @NotNull(message = "请选择帖子分类")
    private Long categoryId;

    /** 配图URL列表(由 /post/image 上传后返回) */
    @Size(max = 9, message = "最多上传9张配图")
    private List<String> imgUrls;
}
