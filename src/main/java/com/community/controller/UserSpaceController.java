package com.community.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.community.common.api.R;
import com.community.common.util.IpUtil;
import com.community.dto.GuestbookDTO;
import com.community.dto.ProfileReportDTO;
import com.community.dto.SpaceLayoutDTO;
import com.community.service.GuestbookService;
import com.community.service.ProfileSpaceService;
import com.community.vo.GuestbookVO;
import com.community.vo.PageVO;
import com.community.vo.SpaceVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/** 可配置个人主页接口（公开浏览 + 本人编辑 + 留言/举报）。 */
@RestController
@RequiredArgsConstructor
public class UserSpaceController {

    private final ProfileSpaceService spaceService;
    private final GuestbookService guestbookService;

    /** 公开主页：布局 + 统计 + 访问量（同 IP 5 分钟去重） */
    @GetMapping("/user/{id}/space")
    public R<SpaceVO> space(@PathVariable Long id, HttpServletRequest request) {
        Long viewer = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        return R.ok(spaceService.getSpace(id, viewer, IpUtil.clientIp(request)));
    }

    /** 保存自己的主页布局（乐观锁，版本不一致返回 409） */
    @SaCheckLogin
    @PutMapping("/user/profile/space")
    public R<Map<String, Integer>> saveSpace(@Valid @RequestBody SpaceLayoutDTO dto) {
        int version = spaceService.saveSpace(StpUtil.getLoginIdAsLong(), dto.getLayout(), dto.getVersion());
        return R.ok(Map.of("version", version));
    }

    /** 主页图片上传（头图/作品封面/图集） */
    @SaCheckLogin
    @PostMapping("/user/profile/image")
    public R<String> uploadImage(@RequestParam("file") MultipartFile file) {
        return R.ok(spaceService.uploadImage(file));
    }

    /** 留言列表（游客可看） */
    @GetMapping("/user/{id}/guestbook")
    public R<PageVO<GuestbookVO>> guestbook(@PathVariable Long id,
                                            @RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "10") long size) {
        return R.ok(guestbookService.list(id, page, size));
    }

    /** 发表留言（登录；1 分钟 1 条） */
    @SaCheckLogin
    @PostMapping("/user/{id}/guestbook")
    public R<GuestbookVO> addGuestbook(@PathVariable Long id, @Valid @RequestBody GuestbookDTO dto) {
        return R.ok(guestbookService.add(id, StpUtil.getLoginIdAsLong(), dto.getContent()));
    }

    /** 删除留言（留言人 / 主页主人 / 管理员） */
    @SaCheckLogin
    @DeleteMapping("/user/guestbook/{id}")
    public R<Void> deleteGuestbook(@PathVariable Long id) {
        guestbookService.delete(id, StpUtil.getLoginIdAsLong(), StpUtil.hasRole("admin"));
        return R.ok();
    }

    /** 举报整页或某个模块（登录；1 分钟 1 条） */
    @SaCheckLogin
    @PostMapping("/user/{id}/report")
    public R<Void> report(@PathVariable Long id, @Valid @RequestBody ProfileReportDTO dto) {
        spaceService.report(StpUtil.getLoginIdAsLong(), id, dto.getModuleType(), dto.getReason());
        return R.ok();
    }
}
