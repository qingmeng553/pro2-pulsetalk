package com.community.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.community.entity.UserFriend;
import org.apache.ibatis.annotations.Mapper;

/**
 * 好友关系 Mapper
 */
@Mapper
public interface UserFriendMapper extends BaseMapper<UserFriend> {
}
