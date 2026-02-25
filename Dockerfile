FROM eclipse-temurin:25.0.2_10-jre-noble

RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends tzdata && \
    rm -rf /var/lib/apt/lists/*

ENV TZ=Asia/Seoul

WORKDIR /app

COPY ./build/libs/greenlight-scheduler-1.0.0.jar /app/greenlight-scheduler.jar

EXPOSE 27070

ENTRYPOINT ["java", "-jar", "greenlight-scheduler.jar"]