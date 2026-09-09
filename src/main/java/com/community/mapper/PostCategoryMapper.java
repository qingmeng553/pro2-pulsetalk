package com.community.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.community.entity.PostCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 帖子分类 Mapper
 */
@Mapper
public interface PostCategoryMapper extends BaseMapper<PostCategory> {
}
