# ============================================================
# pro2 点评贴吧社区 后端镜像
# 约束：不在容器内执行 Maven 编译；先在本机执行
#     mvn clean install -DskipTests
# 生成 target/pro2-1.0.0.jar 后再构建本镜像（仅复制并运行 jar）
# ============================================================
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="pro2-community" \
      description="点评贴吧社区后端应用 (SpringBoot3 + Sa-Token + MyBatis-Plus)"

# 工作目录
WORKDIR /app

# 仅复制本机已打好的可执行 jar
COPY target/pro2-1.0.0.jar app.jar

# 运行端口（与 application*.yml 中 server.port 保持一致）
EXPOSE 8085

# 容器健康检查：探测 /demo/ping（busybox wget 可用于 alpine）
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=5 \
    CMD wget -q -O - http://127.0.0.1:8085/demo/ping >/dev/null 2>&1 || exit 1

# 启动应用（激活的 profile 由 docker-compose 通过 SPRING_PROFILES_ACTIVE=docker 注入）
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-Xmx512m", "-jar", "app.jar"]
