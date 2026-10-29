package com.community.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.community.entity.UserGuestbook;
import org.apache.ibatis.annotations.Mapper;

/** 主页留言 Mapper。 */
@Mapper
public interface UserGuestbookMapper extends BaseMapper<UserGuestbook> {
}
