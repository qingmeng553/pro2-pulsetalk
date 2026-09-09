package com.community.controller;

import com.community.common.api.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 探测/引导接口
 *
 * <pre>
 * GET /           服务信息与接口引导(访问后端根路径不再 404)
 * GET /demo/ping  存活探测(供 docker healthcheck / 运维探活)
 * </pre>
 */
@RestController
public class DemoController {

    /**
     * 根路径引导：后端本身不承载页面(UI 为独立的 pro2-frontend/Vue3 工程)，
     * 返回服务状态与入口指引，避免访问 8085 根路径时产生误导性的 404。
     */
    @GetMapping("/")
    public R<Map<String, Object>> home() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("service", "PulseTalk");
        data.put("status", "running");
        data.put("description", "Redis 综合实战项目：PulseTalk 社区(帖子 + 私聊 + 好友 + 消息通知)");
        data.put("backendPing", "GET /demo/ping");
        data.put("mainApis", Map.of(
                "登录", "POST /auth/login",
                "帖子列表", "GET /post/list",
                "帖子详情", "GET /post/{id}",
                "热度榜单", "GET /post/hot/rank",
                "评论列表", "GET /post/{postId}/comment/list",
                "分类列表", "GET /post/category/list",
                "消息通知", "GET /notify/list",
                "私信会话", "GET /chat/threads",
                "好友列表", "GET /friend/list"
        ));
        data.put("frontend", "请启动 pro2-frontend 后访问 http://localhost:5173（开发模式经 Vite 代理 /api → 8085）");
        data.put("docs", "详见项目根目录 README.md 与 deploy-guide.md");
        return R.ok(data);
    }

    /** GET /demo/ping 存活探测 */
    @GetMapping("/demo/ping")
    public R<Map<String, String>> ping() {
        return R.ok(Map.of("pong", "ok", "ts", String.valueOf(System.currentTimeMillis())));
    }
}
