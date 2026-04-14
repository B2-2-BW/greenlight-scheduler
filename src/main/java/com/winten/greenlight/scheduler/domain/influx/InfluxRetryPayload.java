package com.winten.greenlight.scheduler.domain.influx;

import com.influxdb.client.write.Point;

import java.util.List;

public record InfluxRetryPayload(
        String bucket,
        String org,
        List<Point> points
) {}