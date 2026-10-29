package com.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.common.api.ResultCode;
import com.community.common.constant.RedisKeys;
import com.community.common.exception.BusinessException;
import com.community.entity.Post;
import com.community.entity.ProfileHiddenModule;
import com.community.entity.ProfileReport;
import com.community.entity.SysUser;
import com.community.entity.UserFriend;
import com.community.mapper.PostMapper;
import com.community.mapper.ProfileHiddenModuleMapper;
import com.community.mapper.ProfileReportMapper;
import com.community.mapper.SysUserMapper;
import com.community.mapper.UserFriendMapper;
import com.community.service.MinioFileService;
import com.community.service.ProfileLayoutValidator;
import com.community.service.ProfileSpaceService;
import com.community.service.RateLimitService;
import com.community.vo.PageVO;
import com.community.vo.ProfileReportVO;
import com.community.vo.SpaceStatsVO;
import com.community.vo.SpaceVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 可配置个人主页实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileSpaceServiceImpl implements ProfileSpaceService {

    private final SysUserMapper userMapper;
    private final PostMapper postMapper;
    private final UserFriendMapper friendMapper;
    private final ProfileHiddenModuleMapper hiddenMapper;
    private final ProfileReportMapper reportMapper;
    private final ProfileLayoutValidator layoutValidator;
    private final MinioFileService minioFileService;
    private final StringRedisTemplate redisTemplate;
    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    @Override
    public SpaceVO getSpace(long targetId, Long viewerId, String ip) {
        SysUser user = requireUser(targetId);

        SpaceVO vo = new SpaceVO();
        vo.setUserId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setRole(user.getRole());
        vo.setCreateTime(user.getCreateTime());
        vo.setVersion(user.getProfileVersion() == null ? 0 : user.getProfileVersion());

        JsonNode layout;
        try {
            layout = StringUtils.hasText(user.getProfileLayout())
                    ? objectMapper.readTree(user.getProfileLayout())
                    : layoutValidator.defaultLayout();
        } catch (Exception e) {
            log.warn("[主页] 用户 {} 布局解析失败，回退默认布局", targetId);
            layout = layoutValidator.defaultLayout();
        }
        vo.setLayout(layout);

        List<String> hidden = hiddenMapper.selectList(new LambdaQueryWrapper<ProfileHiddenModule>()
                        .eq(ProfileHiddenModule::getUserId, targetId))
                .stream().map(ProfileHiddenModule::getModuleType).toList();
        vo.setHiddenModules(hidden);

        SpaceStatsVO stats = new SpaceStatsVO();
        stats.setPostCount(postMapper.selectCount(new LambdaQueryWrapper<Post>()
                .eq(Post::getUserId, targetId)));
        Long likes = postMapper.sumLikeCountByUser(targetId);
        stats.setLikeCount(likes == null ? 0L : likes);
        stats.setFriendCount(friendMapper.selectCount(new LambdaQueryWrapper<UserFriend>()
                .eq(UserFriend::getStatus, UserFriend.STATUS_ACCEPTED)
                .and(w -> w.eq(UserFriend::getUserId, targetId).or().eq(UserFriend::getFriendId, targetId))));
        stats.setViewCount(increaseAndGetView(targetId, viewerId, ip));
        vo.setStats(stats);
        return vo;
    }

    @Override
    public int saveSpace(long userId, JsonNode layout, Integer version) {
        SysUser user = requireUser(userId);
        int current = user.getProfileVersion() == null ? 0 : user.getProfileVersion();
        if (version == null || version != current) {
            throw new BusinessException(409, "布局已更新，请刷新后重试");
        }
        JsonNode normalized = layoutValidator.normalize(userId, layout);

        String json;
        try {
            json = objectMapper.writeValueAsString(normalized);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.ERROR, "布局保存失败");
        }

        int rows = userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, userId)
                .eq(SysUser::getProfileVersion, current)
                .set(SysUser::getProfileLayout, json)
                .set(SysUser::getProfileVersion, current + 1));
        if (rows == 0) {
            throw new BusinessException(409, "布局已更新，请刷新后重试");
        }
        log.info("[主页] 用户 {} 保存布局，版本 {} -> {}", userId, current, current + 1);
        return current + 1;
    }

    @Override
    public String uploadImage(MultipartFile file) {
        return minioFileService.uploadProfileImage(file);
    }

    @Override
    public void hideModule(long adminId, long targetId, String moduleType, boolean hidden, String reason) {
        if ("bio".equals(moduleType) || !ProfileLayoutValidator.MODULE_TYPES.contains(moduleType)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该模块不支持隐藏");
        }
        requireUser(targetId);

        ProfileHiddenModule existing = hiddenMapper.selectOne(new LambdaQueryWrapper<ProfileHiddenModule>()
                .eq(ProfileHiddenModule::getUserId, targetId)
                .eq(ProfileHiddenModule::getModuleType, moduleType));
        if (hidden) {
            if (existing != null) {
                existing.setHiddenBy(adminId);
                existing.setReason(reason);
                hiddenMapper.updateById(existing);
            } else {
                ProfileHiddenModule row = new ProfileHiddenModule();
                row.setUserId(targetId);
                row.setModuleType(moduleType);
                row.setHiddenBy(adminId);
                row.setReason(reason);
                try {
                    hiddenMapper.insert(row);
                } catch (DuplicateKeyException ignore) {
                    // 并发下已存在，视为成功
                }
            }
        } else if (existing != null) {
            hiddenMapper.deleteById(existing.getId());
        }
        log.info("[主页] 管理员 {} {} 模块 {}(用户 {})", adminId, hidden ? "隐藏" : "恢复", moduleType, targetId);
    }

    @Override
    public void report(long reporterId, long targetId, String moduleType, String reason) {
        if (reporterId == targetId) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能举报自己");
        }
        requireUser(targetId);
        if (!"profile".equals(moduleType) && !ProfileLayoutValidator.MODULE_TYPES.contains(moduleType)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "举报对象不合法");
        }
        rateLimitService.check(RedisKeys.reportLimitKey(reporterId), 1, 60);

        ProfileReport report = new ProfileReport();
        report.setTargetUserId(targetId);
        report.setReporterId(reporterId);
        report.setModuleType(moduleType);
        report.setReason(reason.trim());
        report.setStatus(0);
        reportMapper.insert(report);
    }

    @Override
    public PageVO<ProfileReportVO> listReports(long page, long size, Integer status) {
        Page<ProfileReport> p = reportMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<ProfileReport>()
                        .eq(status != null, ProfileReport::getStatus, status)
                        .orderByAsc(ProfileReport::getStatus)
                        .orderByDesc(ProfileReport::getCreateTime));

        List<Long> ids = p.getRecords().stream()
                .flatMap(r -> java.util.stream.Stream.of(r.getTargetUserId(), r.getReporterId()))
                .distinct().toList();
        Map<Long, SysUser> users = ids.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity(), (a, b) -> a));
        return PageVO.of(p, r -> toReportVO(r, users));
    }

    @Override
    public void handleReport(long adminId, long reportId) {
        ProfileReport report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "举报记录不存在");
        }
        report.setStatus(1);
        report.setHandleBy(adminId);
        report.setHandleTime(LocalDateTime.now());
        reportMapper.updateById(report);
    }

    private long increaseAndGetView(long targetId, Long viewerId, String ip) {
        String viewKey = RedisKeys.profileViewKey(targetId);
        if (viewerId == null || viewerId != targetId) {
            boolean count = true;
            if (StringUtils.hasText(ip)) {
                Boolean first = redisTemplate.opsForValue().setIfAbsent(
                        RedisKeys.profileViewDedupeKey(targetId, ip), "1", Duration.ofMinutes(5));
                count = Boolean.TRUE.equals(first);
            }
            if (count) {
                redisTemplate.opsForValue().increment(viewKey);
            }
        }
        String raw = redisTemplate.opsForValue().get(viewKey);
        if (raw == null) {
            return 0L;
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private SysUser requireUser(long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在或已被删除");
        }
        return user;
    }

    private ProfileReportVO toReportVO(ProfileReport report, Map<Long, SysUser> users) {
        ProfileReportVO vo = new ProfileReportVO();
        vo.setId(report.getId());
        vo.setTargetUserId(report.getTargetUserId());
        vo.setReporterId(report.getReporterId());
        vo.setModuleType(report.getModuleType());
        vo.setReason(report.getReason());
        vo.setStatus(report.getStatus());
        vo.setCreateTime(report.getCreateTime());
        vo.setHandleTime(report.getHandleTime());
        SysUser target = users.get(report.getTargetUserId());
        SysUser reporter = users.get(report.getReporterId());
        vo.setTargetNickname(target == null ? null : target.getNickname());
        vo.setReporterNickname(reporter == null ? null : reporter.getNickname());
        return vo;
    }
}
