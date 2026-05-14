# ==================== 构建阶段 ====================
FROM maven:3.9-eclipse-temurin-17-alpine AS build

WORKDIR /build

# 先复制 pom.xml 和 src 源码，利用 Docker 缓存层
COPY pom.xml .
COPY src ./src

# 构建项目（跳过测试）
RUN mvn clean package -DskipTests -q

# ==================== 运行阶段 ====================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 创建必要的目录
RUN mkdir -p /app/data /app/logs /app/video-store

# 从构建阶段复制 jar
COPY --from=build /build/target/*.jar /app/app.jar

# 暴露应用端口
EXPOSE 8080

# 启动应用（支持 JAVA_OPTS 环境变量）
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
