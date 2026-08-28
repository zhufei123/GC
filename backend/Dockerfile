# 构建：cd backend && mvn -q -DskipTests package
# 打包产物：recycle-admin-api/target/backend.jar（spring-boot repackage fat jar）
FROM eclipse-temurin:21-jre-jammy

ENV TZ=Asia/Shanghai \
    JAVA_OPTS=""

WORKDIR /app

COPY recycle-admin-api/target/backend.jar /app/app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -fs http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
