package com.community;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** PulseTalk 启动类。 */
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
