# ── 1단계: 빌드 스테이지 ──
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app

COPY gradle/ gradle/
COPY gradlew build.gradle.kts settings.gradle.kts ./

RUN sed -i 's/\r$//' ./gradlew && chmod +x ./gradlew
RUN ./gradlew dependencies --no-daemon

COPY src ./src
RUN ./gradlew clean bootJar --no-daemon -x test

# ── 2단계: 실행 스테이지 ──
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
