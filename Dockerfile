# ---- build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B -q -DskipTests package

# ---- runtime stage ----
FROM eclipse-temurin:17-jre-jammy
ARG APP_USER=clearhub
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl tzdata \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -r ${APP_USER} \
    && useradd -r -g ${APP_USER} ${APP_USER} \
    && mkdir -p /app/logs \
    && chown -R ${APP_USER}:${APP_USER} /app

COPY --from=build /build/target/trade-core.jar /app/trade-core.jar

ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms256m -Xmx512m" \
    SERVER_PORT=8080

USER ${APP_USER}
EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=45s --retries=8 \
  CMD curl -fsS "http://127.0.0.1:${SERVER_PORT}/actuator/health" || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/trade-core.jar"]
