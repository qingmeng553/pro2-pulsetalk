package com.community.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.community.common.api.R;
import com.community.dto.HideModuleDTO;
import com.community.service.ProfileSpaceService;
import com.community.vo.PageVO;
import com.community.vo.ProfileReportVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 管理端 - 主页举报与模块隐藏。 */
@RestController
@RequiredArgsConstructor
public class AdminSpaceController {

    private final ProfileSpaceService spaceService;

    /** 举报列表（status 可选：0 待处理 / 1 已处理） */
    @SaCheckRole("admin")
    @GetMapping("/admin/space/reports")
    public R<PageVO<ProfileReportVO>> reports(@RequestParam(defaultValue = "1") long page,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) Integer status) {
        return R.ok(spaceService.listReports(page, size, status));
    }

    /** 标记举报已处理 */
    @SaCheckRole("admin")
    @PostMapping("/admin/space/reports/{id}/handle")
    public R<Void> handle(@PathVariable Long id) {
        spaceService.handleReport(StpUtil.getLoginIdAsLong(), id);
        return R.ok();
    }

    /** 隐藏/恢复某个用户的主页模块 */
    @SaCheckRole("admin")
    @PostMapping("/admin/user/{id}/space/hide")
    public R<Void> hideModule(@PathVariable Long id, @Valid @RequestBody HideModuleDTO dto) {
        spaceService.hideModule(StpUtil.getLoginIdAsLong(), id, dto.getModuleType(),
                Boolean.TRUE.equals(dto.getHidden()), dto.getReason());
        return R.ok();
    }
}
