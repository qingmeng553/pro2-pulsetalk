package com.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.common.api.ResultCode;
import com.community.common.exception.BusinessException;
import com.community.dto.CommentDTO;
import com.community.entity.Post;
import com.community.entity.PostComment;
import com.community.entity.SysUser;
import com.community.mapper.PostCommentMapper;
import com.community.mapper.PostMapper;
import com.community.mapper.SysUserMapper;
import com.community.service.CommentService;
import com.community.service.NotifyService;
import com.community.service.RateLimitService;
import com.community.vo.AuthorVO;
import com.community.vo.CommentVO;
import com.community.vo.PageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 评论服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final PostCommentMapper commentMapper;
    private final PostMapper postMapper;
    private final SysUserMapper userMapper;
    private final RateLimitService rateLimitService;
    private final NotifyService notifyService;

    @Override
    public PageVO<CommentVO> pageList(long postId, long page, long size) {
        // 按发帖顺序(时间正序)分页，楼层号稳定递增
        Page<PostComment> p = commentMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<PostComment>()
                        .eq(PostComment::getPostId, postId)
                        .orderByAsc(PostComment::getCreateTime)
                        .orderByAsc(PostComment::getId));

        List<PostComment> records = p.getRecords();
        if (CollectionUtils.isEmpty(records)) {
            PageVO<CommentVO> empty = new PageVO<>();
            empty.setRecords(Collections.emptyList());
            empty.setTotal(p.getTotal());
            empty.setCurrent(p.getCurrent());
            empty.setSize(p.getSize());
            empty.setPages(p.getPages());
            return empty;
        }

        // 批量查询评论人信息(一次查询，避免 N+1)
        List<Long> userIds = records.stream().map(PostComment::getUserId).distinct().toList();
        Map<Long, SysUser> userMap = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity(), (a, b) -> a));

        long offset = (page - 1) * size;
        List<CommentVO> voList = new java.util.ArrayList<>(records.size());
        for (int i = 0; i < records.size(); i++) {
            PostComment c = records.get(i);
            CommentVO vo = new CommentVO();
            vo.setId(c.getId());
            vo.setPostId(c.getPostId());
            vo.setUserId(c.getUserId());
            vo.setContent(c.getContent());
            vo.setCreateTime(c.getCreateTime());
            // 楼层号: 首页第一条为1楼
            vo.setFloor((int) (offset + i + 1));
            vo.setAuthor(toAuthor(userMap.get(c.getUserId())));
            voList.add(vo);
        }
        PageVO<CommentVO> result = new PageVO<>();
        result.setRecords(voList);
        result.setTotal(p.getTotal());
        result.setCurrent(p.getCurrent());
        result.setSize(p.getSize());
        result.setPages(p.getPages());
        return result;
    }

    @Override
    public void create(long userId, long postId, CommentDTO dto) {
        // 评论接口限流(Redis 计数器)
        rateLimitService.checkComment(userId);

        // 帖子必须存在(未被删除)
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在或已被删除");
        }

        PostComment comment = new PostComment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(dto.getContent().trim());
        commentMapper.insert(comment);

        // v2 消息通知：评论了你的帖子(带内容预览，自己评论自己不发)
        String preview = comment.getContent().length() > 80
                ? comment.getContent().substring(0, 80) + "…"
                : comment.getContent();
        notifyService.onPostCommented(post.getUserId(), userId, postId, comment.getId(), preview);
        log.info("[评论] 用户 {} 评论帖子 {} -> commentId={}", userId, postId, comment.getId());
    }

    @Override
    public void deleteOwn(long userId, long commentId) {
        PostComment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评论不存在或已被删除");
        }
        if (!Objects.equals(comment.getUserId(), userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能删除自己发布的评论");
        }
        // 逻辑软删除 is_deleted=1
        commentMapper.deleteById(commentId);
        log.info("[评论] 用户 {} 删除自己的评论 commentId={}", userId, commentId);
    }

    @Override
    public void adminDelete(long commentId) {
        PostComment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评论不存在或已被删除");
        }
        commentMapper.deleteById(commentId);
        log.info("[评论] 管理员删除任意评论 commentId={}", commentId);
    }

    /** 用户实体 → 作者摘要(用户异常缺失时兜底为“已注销用户”) */
    private AuthorVO toAuthor(SysUser user) {
        AuthorVO vo = new AuthorVO();
        if (user == null) {
            vo.setNickname("已注销用户");
            return vo;
        }
        vo.setUserId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setRole(user.getRole());
        return vo;
    }
}
