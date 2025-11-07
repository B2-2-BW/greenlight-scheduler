package com.winten.greenlight.scheduler.db.config;

import com.influxdb.client.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InfluxDBConfig {

    @Value("${influxdb.url}")
    private String influxUrl;
    @Value("${influxdb.token}")
    private String token;
    @Value("${influxdb.org}")
    private String org;
    @Value("${influxdb.bucket}")
    private String bucket;

    @Bean(destroyMethod = "close")
    public InfluxDBClient influxDBClient() {
        InfluxDBClientOptions options = InfluxDBClientOptions.builder()
                .url(influxUrl)
                .authenticateToken(token.toCharArray())
                .org(org)
                .bucket(bucket)
                .build();
        return InfluxDBClientFactory.create(options);
    }

    @Bean
    public WriteApi influxWriteApi(InfluxDBClient client) {
        // 반응형 쓰기 API를 Bean으로 등록
        return client.makeWriteApi();
    }

    @Bean
    public QueryApi queryApi(InfluxDBClient client) {
        return client.getQueryApi();
    }
}