# ==================== 构建阶段 ====================
# swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/maven:3.9-eclipse-temurin-21-alpine
FROM swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /build

# 先复制 pom.xml 和 src 源码，利用 Docker 缓存层
COPY pom.xml .
COPY src ./src

# 复制HanimeClient依赖 jar（注意文件名与路径）
COPY HanimeClient-2.6-SNAPSHOT.jar /tmp/

# 手动安装到容器内的 Maven 本地仓库
RUN mvn install:install-file \
    -Dfile=/tmp/HanimeClient-2.6-SNAPSHOT.jar \
    -DgroupId=com.shikou \
    -DartifactId=HanimeClient \
    -Dversion=2.6-SNAPSHOT \
    -Dpackaging=jar

# 构建项目（跳过测试）
RUN mvn clean package -DskipTests -q

# ==================== 运行阶段 ====================
# swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/library/eclipse-temurin:21-jre-alpine
FROM swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/library/eclipse-temurin:21-jre-alpine

WORKDIR /app

# 创建必要的目录
RUN mkdir -p /app/data /app/logs /app/video-store

# 从构建阶段复制 jar
COPY --from=build /build/target/*.jar /app/app.jar

# 暴露应用端口
EXPOSE 8080

# 启动应用（支持 JAVA_OPTS 环境变量）
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=docker -jar /app/app.jar"]
