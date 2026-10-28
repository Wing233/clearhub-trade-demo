FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl tzdata \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /build/target/clearing-adapter.jar /app/clearing-adapter.jar
ENV TZ=Asia/Shanghai JAVA_OPTS="-Xms128m -Xmx256m"
EXPOSE 8090
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=8 \
  CMD curl -fsS http://127.0.0.1:8090/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/clearing-adapter.jar"]
