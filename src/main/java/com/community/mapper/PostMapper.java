package com.community.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.community.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 帖子 Mapper。 */
@Mapper
public interface PostMapper extends BaseMapper<Post> {

    /** 某用户收到的总点赞数（主页 stats 模块） */
    @Select("SELECT COALESCE(SUM(like_count), 0) FROM post WHERE user_id = #{userId} AND is_deleted = 0")
    Long sumLikeCountByUser(@Param("userId") Long userId);
}
