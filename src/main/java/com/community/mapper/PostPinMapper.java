package com.community.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.community.entity.PostPin;
import org.apache.ibatis.annotations.Mapper;

/**
 * 置顶规则帖 Mapper
 */
@Mapper
public interface PostPinMapper extends BaseMapper<PostPin> {
}
