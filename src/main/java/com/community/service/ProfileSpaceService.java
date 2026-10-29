package com.community.service;

import com.community.vo.PageVO;
import com.community.vo.ProfileReportVO;
import com.community.vo.SpaceVO;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.multipart.MultipartFile;

/** 可配置个人主页：读取/保存/图片/举报/管理员隐藏。 */
public interface ProfileSpaceService {

    SpaceVO getSpace(long targetId, Long viewerId, String ip);

    int saveSpace(long userId, JsonNode layout, Integer version);

    String uploadImage(MultipartFile file);

    void hideModule(long adminId, long targetId, String moduleType, boolean hidden, String reason);

    void report(long reporterId, long targetId, String moduleType, String reason);

    PageVO<ProfileReportVO> listReports(long page, long size, Integer status);

    void handleReport(long adminId, long reportId);
}
