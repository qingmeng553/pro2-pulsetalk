package com.community.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.community.entity.UserBlacklist;
import org.apache.ibatis.annotations.Mapper;

/**
 * 黑名单 Mapper
 */
@Mapper
public interface UserBlacklistMapper extends BaseMapper<UserBlacklist> {
}
