FROM amazoncorretto:25.0.5-al2023-headless

RUN dnf install -y tzdata && dnf clean all

ENV TZ=Asia/Seoul

WORKDIR /app

COPY ./build/libs/greenlight-scheduler-1.0.0.jar /app/greenlight-scheduler.jar

EXPOSE 27070

ENTRYPOINT ["java", "-jar", "greenlight-scheduler.jar"]
