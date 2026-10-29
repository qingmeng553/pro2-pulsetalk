package com.community.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.common.api.ResultCode;
import com.community.common.exception.BusinessException;
import com.community.entity.Post;
import com.community.mapper.PostMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 主页布局 schema 校验 + 消毒（白名单，未知值直接拒绝）。 */
@Component
@RequiredArgsConstructor
public class ProfileLayoutValidator {

    public static final Set<String> MODULE_TYPES = Set.of(
            "bio", "pinned", "skills", "projects", "gallery", "links", "posts", "stats", "guestbook");

    public static final Set<String> THEME_PACKS = Set.of(
            "minimal-blue", "warm-paper", "forest", "slate", "rose",
            "ocean", "lavender", "sand", "dark-ink", "dark-graphite");

    public static final Set<String> CARD_STYLES = Set.of("border", "flat");

    public static final Set<String> WIDGETS = Set.of("none", "spark", "leaf", "grid", "star", "wave");

    public static final Set<String> ACCENTS = Set.of(
            "#2563eb", "#7c3aed", "#e11d48", "#d97706",
            "#16a34a", "#0d9488", "#475569", "#dc2626",
            // 主题包内置强调色（也允许用户选）
            "#b45309", "#15803d", "#be123c", "#0e7490",
            "#6d28d9", "#a16207", "#60a5fa", "#f59e0b");

    public static final Set<String> LINK_ICONS = Set.of(
            "github", "bilibili", "juejin", "zhihu", "weibo", "email", "website", "other");

    private static final int MAX_JSON_BYTES = 32 * 1024;

    private final ObjectMapper objectMapper;
    private final PostMapper postMapper;

    private JsonNode defaultLayout;

    @PostConstruct
    void init() {
        try (InputStream in = getClass().getResourceAsStream("/space/default-layout.json")) {
            defaultLayout = objectMapper.readTree(in);
        } catch (Exception e) {
            throw new IllegalStateException("默认主页布局加载失败", e);
        }
    }

    /** 默认布局（新用户/未保存过） */
    public JsonNode defaultLayout() {
        return defaultLayout.deepCopy();
    }

    /**
     * 校验并返回规范化后的布局；任何非法值直接 400，不静默写入。
     */
    public JsonNode normalize(long userId, JsonNode input) {
        if (input == null || !input.isObject()) {
            throw bad("布局格式不正确");
        }
        if (input.path("v").asInt(0) != 2) {
            throw bad("布局版本不支持");
        }

        ObjectNode out = objectMapper.createObjectNode();
        out.put("v", 2);
        out.set("theme", normalizeTheme(input.get("theme")));

        JsonNode modulesNode = input.get("modules");
        if (modulesNode == null || !modulesNode.isArray()) {
            throw bad("缺少 modules");
        }
        if (modulesNode.size() < 1 || modulesNode.size() > MODULE_TYPES.size()) {
            throw bad("模块数量不合法");
        }

        List<ObjectNode> modules = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (JsonNode node : modulesNode) {
            if (!node.isObject()) {
                throw bad("模块格式不正确");
            }
            String type = node.path("type").asText("");
            if (!MODULE_TYPES.contains(type)) {
                throw bad("模块类型不支持: " + type);
            }
            if (!seen.add(type)) {
                throw bad("模块重复: " + type);
            }
            ObjectNode module = objectMapper.createObjectNode();
            module.put("type", type);
            module.put("order", node.path("order").asInt(99));
            boolean visible = node.path("visible").asBoolean(true);
            if ("bio".equals(type)) {
                visible = true;
            }
            module.put("visible", visible);
            String width = node.path("width").asText("full");
            if (!"full".equals(width) && !"half".equals(width)) {
                width = "full";
            }
            if ("bio".equals(type)) {
                width = "full";
            }
            module.put("width", width);
            JsonNode data = normalizeData(userId, type, node.get("data"));
            if (data != null) {
                module.set("data", data);
            }
            modules.add(module);
        }

        if (!seen.contains("bio")) {
            throw bad("bio 模块不能删除");
        }

        modules.sort(Comparator.comparingInt(m -> m.path("order").asInt(99)));
        ArrayNode array = objectMapper.createArrayNode();
        for (int i = 0; i < modules.size(); i++) {
            modules.get(i).put("order", i + 1);
            array.add(modules.get(i));
        }
        out.set("modules", array);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(out);
            if (bytes.length > MAX_JSON_BYTES) {
                throw bad("布局超过 32KB");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw bad("布局序列化失败");
        }
        return out;
    }

    private ObjectNode normalizeTheme(JsonNode themeNode) {
        if (themeNode != null && !themeNode.isObject()) {
            throw bad("theme 格式不正确");
        }
        String pack = text(themeNode, "pack", 32);
        if (pack.isEmpty()) {
            pack = "minimal-blue";
        }
        if (!THEME_PACKS.contains(pack)) {
            throw bad("主题包不支持: " + pack);
        }
        String cardStyle = text(themeNode, "cardStyle", 16);
        if (cardStyle.isEmpty()) {
            cardStyle = "border";
        }
        if (!CARD_STYLES.contains(cardStyle)) {
            throw bad("卡片样式不支持");
        }
        String widget = text(themeNode, "widget", 16);
        if (widget.isEmpty()) {
            widget = "none";
        }
        if (!WIDGETS.contains(widget)) {
            throw bad("挂件不支持");
        }
        String accent = text(themeNode, "accent", 16).toLowerCase(Locale.ROOT);
        if (accent.isEmpty()) {
            accent = "#2563eb";
        }
        if (!ACCENTS.contains(accent)) {
            throw bad("强调色不在预设色板内");
        }

        ObjectNode out = objectMapper.createObjectNode();
        out.put("pack", pack);
        String header = text(themeNode, "header", 512);
        if (header.isEmpty()) {
            out.putNull("header");
        } else {
            out.put("header", imageUrl(header));
        }
        out.put("cardStyle", cardStyle);
        out.put("widget", widget);
        out.put("accent", accent);
        return out;
    }

    private JsonNode normalizeData(long userId, String type, JsonNode data) {
        return switch (type) {
            case "bio" -> {
                ObjectNode out = objectMapper.createObjectNode();
                String intro = text(data, "intro", 500);
                // 保留 Markdown 原文（含换行）；拒绝明显危险片段，最终渲染由前端 DOMPurify 兜底
                String lower = intro.toLowerCase(Locale.ROOT);
                if (lower.contains("<script") || lower.contains("<iframe")
                        || lower.contains("javascript:") || lower.contains("vbscript:")
                        || lower.contains("data:") || lower.contains("onerror=")
                        || lower.contains("onload=") || lower.contains("onclick=")) {
                    throw bad("简介包含非法内容");
                }
                out.put("intro", intro);
                yield out;
            }
            case "pinned" -> normalizePinned(userId, data);
            case "skills" -> normalizeSkills(data);
            case "projects" -> normalizeProjects(data);
            case "gallery" -> normalizeGallery(data);
            case "links" -> normalizeLinks(data);
            default -> null;
        };
    }

    private JsonNode normalizePinned(long userId, JsonNode data) {
        if (data == null || !data.isObject()) {
            return null;
        }
        JsonNode pid = data.get("postId");
        if (pid == null || pid.isNull() || pid.asText().isBlank()) {
            return null;
        }
        long postId;
        try {
            postId = Long.parseLong(pid.asText().trim());
        } catch (NumberFormatException e) {
            throw bad("置顶帖子 ID 不合法");
        }
        Post post = postMapper.selectOne(new LambdaQueryWrapper<Post>()
                .eq(Post::getId, postId)
                .eq(Post::getUserId, userId));
        if (post == null) {
            throw bad("置顶帖子不存在或不属于你");
        }
        ObjectNode out = objectMapper.createObjectNode();
        out.put("postId", String.valueOf(postId));
        return out;
    }

    private JsonNode normalizeSkills(JsonNode data) {
        if (data == null || !data.isArray()) {
            return objectMapper.createArrayNode();
        }
        if (data.size() > 20) {
            throw bad("技能标签最多 20 个");
        }
        ArrayNode out = objectMapper.createArrayNode();
        for (JsonNode item : data) {
            String skill = item.asText("").trim();
            if (skill.isEmpty()) {
                continue;
            }
            if (skill.length() > 12) {
                throw bad("单个技能标签最多 12 字");
            }
            out.add(skill);
        }
        return out;
    }

    private JsonNode normalizeProjects(JsonNode data) {
        if (data == null || !data.isArray()) {
            return objectMapper.createArrayNode();
        }
        if (data.size() > 6) {
            throw bad("作品最多 6 个");
        }
        ArrayNode out = objectMapper.createArrayNode();
        for (JsonNode item : data) {
            if (!item.isObject()) {
                continue;
            }
            String title = text(item, "title", 60);
            if (title.isEmpty()) {
                continue;
            }
            ObjectNode project = objectMapper.createObjectNode();
            project.put("title", title);
            project.put("desc", text(item, "desc", 200));

            String url = text(item, "url", 300);
            if (!url.isEmpty()) {
                project.put("url", httpUrl(url));
            }

            String cover = text(item, "cover", 512);
            if (!cover.isEmpty()) {
                project.put("cover", imageUrl(cover));
            }
            out.add(project);
        }
        return out;
    }

    private JsonNode normalizeGallery(JsonNode data) {
        if (data == null || !data.isArray()) {
            return objectMapper.createArrayNode();
        }
        if (data.size() > 9) {
            throw bad("图集最多 9 张");
        }
        ArrayNode out = objectMapper.createArrayNode();
        for (JsonNode item : data) {
            if (!item.isObject()) {
                continue;
            }
            String url = text(item, "url", 512);
            if (url.isEmpty()) {
                continue;
            }
            ObjectNode image = objectMapper.createObjectNode();
            image.put("url", imageUrl(url));
            image.put("caption", text(item, "caption", 60));
            out.add(image);
        }
        return out;
    }

    private JsonNode normalizeLinks(JsonNode data) {
        if (data == null || !data.isArray()) {
            return objectMapper.createArrayNode();
        }
        if (data.size() > 10) {
            throw bad("外链最多 10 条");
        }
        ArrayNode out = objectMapper.createArrayNode();
        for (JsonNode item : data) {
            if (!item.isObject()) {
                continue;
            }
            String name = text(item, "name", 20);
            String url = text(item, "url", 300);
            if (name.isEmpty() || url.isEmpty()) {
                continue;
            }
            String icon = text(item, "icon", 16);
            if (icon.isEmpty() || !LINK_ICONS.contains(icon)) {
                icon = "other";
            }
            ObjectNode link = objectMapper.createObjectNode();
            link.put("name", name);
            link.put("url", httpUrl(url));
            link.put("icon", icon);
            out.add(link);
        }
        return out;
    }

    private String text(JsonNode node, String field, int max) {
        if (node == null) {
            return "";
        }
        String value = node.path(field).asText("").trim();
        if (value.length() > max) {
            throw bad(field + " 超过长度限制");
        }
        return value;
    }

    private String imageUrl(String raw) {
        String value = raw.trim();
        if (!value.startsWith("/files/") || value.contains("..") || value.length() > 512) {
            throw bad("图片必须是本站 /files 地址");
        }
        return value;
    }

    private String httpUrl(String raw) {
        String value = raw.trim();
        if (value.length() > 300) {
            throw bad("链接过长");
        }
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            if (scheme == null || uri.getHost() == null) {
                throw bad("链接格式不正确");
            }
            scheme = scheme.toLowerCase(Locale.ROOT);
            if (!"http".equals(scheme) && !"https".equals(scheme)) {
                throw bad("链接只允许 http/https");
            }
            return value;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw bad("链接格式不正确");
        }
    }

    private BusinessException bad(String message) {
        return new BusinessException(ResultCode.BAD_REQUEST, message);
    }
}
