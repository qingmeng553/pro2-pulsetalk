package com.community;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * pro2 点评贴吧社区 —— 启动类
 *
 * <p>Redis 综合实战项目：仿贴吧社区，整合 Sa-Token(Redis会话) + MyBatis-Plus + MinIO。
 * <ul>
 *   <li>游客只读模式 + 登录写操作，管理员额外拥有软删除任意内容的权限</li>
 *   <li>点赞/收藏/浏览先写 Redis，由定时任务每 5 分钟批量同步 MySQL</li>
 *   <li>Cache-Aside 旁路缓存 + 空值缓存防穿透 + 随机过期防雪崩 + ZSet 热度榜单 + 接口限流</li>
 * </ul>
 */
@SpringBootApplication
@EnableScheduling                       // 开启定时任务(@Scheduled)：点赞/收藏/浏览计数 5 分钟批量落库
@MapperScan("com.community.mapper")     // 扫描 MyBatis-Plus Mapper
public class Pro2Application {

    public static void main(String[] args) {
        SpringApplication.run(Pro2Application.class, args);
        System.out.println("""
                ============================================================
                  PulseTalk 社区后端启动成功
                  本地访问:  http://localhost:8085
                  存活探测:  http://localhost:8085/demo/ping
                ============================================================
                """);
    }
}
