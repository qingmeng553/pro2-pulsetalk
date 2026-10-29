package com.community.controller;

import com.community.common.api.R;
import com.community.vo.AppVersionVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** App 版本检查 / 下载信息（公开）。 */
@RestController
@RequestMapping("/app")
public class AppVersionController {

    @Value("${app.version.latest:1.2.0-alpha.1}")
    private String latestVersion;

    @Value("${app.version.min:1.0.0}")
    private String minVersion;

    @Value("${app.version.url:/download/pulsetalk-1.2.0-alpha.1.apk}")
    private String updateUrl;

    @Value("${app.version.notes:}")
    private String notes;

    @GetMapping("/version")
    public R<AppVersionVO> version() {
        AppVersionVO vo = new AppVersionVO();
        vo.setLatestVersion(latestVersion);
        vo.setMinVersion(minVersion);
        vo.setUpdateUrl(updateUrl);
        vo.setNotes(notes);
        return R.ok(vo);
    }
}
