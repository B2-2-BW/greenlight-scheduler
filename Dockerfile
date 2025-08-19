FROM alpine/java:17.0.12

RUN apk --no-cache add tzdata
ENV TZ=Asia/Seoul

WORKDIR /app

COPY ./build/libs/greenlight-scheduler-1.0.0.jar /app/greenlight-scheduler.jar

EXPOSE 27070

ENTRYPOINT ["java", "-jar", "greenlight-scheduler.jar"]