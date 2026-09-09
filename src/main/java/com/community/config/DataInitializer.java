package com.community.config;

import com.community.common.util.PasswordUtil;
import com.community.entity.PostCategory;
import com.community.entity.SysUser;
import com.community.entity.UserRole;
import com.community.mapper.PostCategoryMapper;
import com.community.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 种子数据初始化器
 *
 * <p>幂等兜底：若库中没有任何用户/分类，则预置 admin(ADMIN)/test(USER) 账号(密码 123456)与默认分类。
 * 与 sql/init.sql 中的预置数据保持等价(密码均为 PasswordUtil 散列)。
 * 仅当数据表已初始化(手动执行过 init.sql)时才会执行；表缺失时静默跳过，不阻塞启动。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final SysUserMapper userMapper;
    private final PostCategoryMapper categoryMapper;

    @Override
    public void run(ApplicationArguments args) {
        try {
            seedUsers();
            seedCategories();
        } catch (Exception e) {
            // 常见于本地库尚未导入表结构：仅提示，不阻塞
            log.warn("[种子数据] 跳过(若本地库未执行 sql/init.sql 建表属正常现象): {}", e.getMessage());
        }
    }

    private void seedUsers() {
        Long count = userMapper.selectCount(null);
        if (count != null && count > 0) {
            return;
        }
        SysUser admin = new SysUser();
        admin.setUsername("admin");
        admin.setPassword(PasswordUtil.encode("123456"));
        admin.setNickname("管理员");
        admin.setRole(UserRole.ADMIN);
        userMapper.insert(admin);

        SysUser test = new SysUser();
        test.setUsername("test");
        test.setPassword(PasswordUtil.encode("123456"));
        test.setNickname("测试用户");
        test.setRole(UserRole.USER);
        userMapper.insert(test);
        log.info("[种子数据] 已预置账号 admin / 123456 (ADMIN)、test / 123456 (USER)");
    }

    private void seedCategories() {
        Long count = categoryMapper.selectCount(null);
        if (count != null && count > 0) {
            return;
        }
        String[] names = {"技术交流", "编程问答", "生活分享", "游戏电竞", "资源干货", "随便聊聊"};
        for (int i = 0; i < names.length; i++) {
            PostCategory category = new PostCategory();
            category.setName(names[i]);
            category.setSort(i + 1);
            categoryMapper.insert(category);
        }
        log.info("[种子数据] 已预置 {} 个默认帖子分类", names.length);
    }
}
