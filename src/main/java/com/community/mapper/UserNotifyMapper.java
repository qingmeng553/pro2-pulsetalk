package com.community.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.community.entity.UserNotify;
import org.apache.ibatis.annotations.Mapper;

/**
 * 站内消息通知 Mapper
 */
@Mapper
public interface UserNotifyMapper extends BaseMapper<UserNotify> {
}
