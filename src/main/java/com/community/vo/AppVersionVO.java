package com.community.vo;

import lombok.Data;

/** App 版本信息（公开接口 /app/version）。 */
@Data
public class AppVersionVO {

    /** 最新版本 */
    private String latestVersion;

    /** 最低支持版本（低于此版本强制更新） */
    private String minVersion;

    /** APK 下载地址（站内相对路径或绝对地址） */
    private String updateUrl;

    /** 更新说明 */
    private String notes;
}
