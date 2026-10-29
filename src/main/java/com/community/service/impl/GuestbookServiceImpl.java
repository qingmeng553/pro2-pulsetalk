package com.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.common.api.ResultCode;
import com.community.common.constant.RedisKeys;
import com.community.common.exception.BusinessException;
import com.community.entity.SysUser;
import com.community.entity.UserGuestbook;
import com.community.mapper.SysUserMapper;
import com.community.mapper.UserGuestbookMapper;
import com.community.service.GuestbookService;
import com.community.service.RateLimitService;
import com.community.vo.AuthorVO;
import com.community.vo.GuestbookVO;
import com.community.vo.PageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 主页留言板实现：登录 + 1 分钟 1 条 + 200 字。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GuestbookServiceImpl implements GuestbookService {

    private final UserGuestbookMapper guestbookMapper;
    private final SysUserMapper userMapper;
    private final RateLimitService rateLimitService;

    @Override
    public PageVO<GuestbookVO> list(long ownerId, long page, long size) {
        requireUser(ownerId);
        Page<UserGuestbook> p = guestbookMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<UserGuestbook>()
                        .eq(UserGuestbook::getOwnerId, ownerId)
                        .orderByDesc(UserGuestbook::getCreateTime)
                        .orderByDesc(UserGuestbook::getId));

        List<Long> userIds = p.getRecords().stream()
                .map(UserGuestbook::getFromUserId).distinct().toList();
        Map<Long, SysUser> users = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity(), (a, b) -> a));
        return PageVO.of(p, row -> toVO(row, users.get(row.getFromUserId())));
    }

    @Override
    public GuestbookVO add(long ownerId, long fromUserId, String content) {
        requireUser(ownerId);
        String text = content == null ? "" : content.trim();
        if (text.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "留言内容不能为空");
        }
        if (text.length() > 200) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "留言最多 200 字");
        }
        rateLimitService.check(RedisKeys.guestbookLimitKey(fromUserId), 1, 60);

        UserGuestbook row = new UserGuestbook();
        row.setOwnerId(ownerId);
        row.setFromUserId(fromUserId);
        row.setContent(text);
        guestbookMapper.insert(row);

        SysUser author = userMapper.selectById(fromUserId);
        log.info("[留言] 用户 {} 给用户 {} 留言", fromUserId, ownerId);
        return toVO(row, author);
    }

    @Override
    public void delete(long guestbookId, long currentUserId, boolean admin) {
        UserGuestbook row = guestbookMapper.selectById(guestbookId);
        if (row == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "留言不存在");
        }
        boolean allowed = admin
                || row.getFromUserId() == currentUserId
                || row.getOwnerId() == currentUserId;
        if (!allowed) {
            throw new BusinessException(ResultCode.FORBIDDEN, "没有权限删除该留言");
        }
        guestbookMapper.deleteById(guestbookId);
        log.info("[留言] 用户 {} 删除留言 {}", currentUserId, guestbookId);
    }

    private SysUser requireUser(long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在或已被删除");
        }
        return user;
    }

    private GuestbookVO toVO(UserGuestbook row, SysUser author) {
        GuestbookVO vo = new GuestbookVO();
        vo.setId(row.getId());
        vo.setContent(row.getContent());
        vo.setCreateTime(row.getCreateTime());
        if (author != null) {
            AuthorVO a = new AuthorVO();
            a.setUserId(author.getId());
            a.setNickname(author.getNickname());
            a.setAvatarUrl(author.getAvatarUrl());
            a.setRole(author.getRole());
            vo.setFromUser(a);
        }
        return vo;
    }
}
