
package com.winten.greenlight.scheduler.domain.actiongroup;

import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.winten.greenlight.scheduler.domain.customer.WaitStatus;
import com.winten.greenlight.scheduler.support.util.RedisKeyBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActionGroupStatusService {
    private final WriteApi influxWriteApi;
    private final RedisKeyBuilder keyBuilder;
    private final RedisTemplate<String, String> redisTemplate;

    public void recordActionGroupStatus() {
        List<ActionGroupSizeMetric> result = new ArrayList<>();

        ScanOptions scanOptions = ScanOptions.scanOptions()
                .match(keyBuilder.actionGroupWaitStatusPattern())
                .count(100)
                .build();

        try (Cursor<byte[]> cursor = redisTemplate.executeWithStickyConnection(
                connection -> connection.keyCommands().scan(scanOptions))
        ) {
            while (cursor.hasNext()) {
                // SCAN 결과는 byte[] 타입이므로 String으로 변환합니다.
                String key = new String(cursor.next(), StandardCharsets.UTF_8);

                // opsForZSet().size()를 호출하여 Sorted Set의 멤버 개수를 가져옵니다.
                Long size = redisTemplate.opsForZSet().size(key);

                if (Objects.nonNull(size) && size > 0) {
                    try {
                        // 키 문자열을 파싱하여 queueId와 waitStatus를 추출합니다.
                        String[] parts = key.split(":");
                        if (parts.length == 5) { // "greenlight:action:queue:id:status" 형식 확인
                            var actionGroupId = Long.parseLong(parts[2]);
                            var waitStatus = WaitStatus.valueOf(parts[4]);
                            var metric = ActionGroupSizeMetric.builder()
                                    .actionGroupId(actionGroupId)
                                    .waitStatus(waitStatus)
                                    .redisKey(key)
                                    .size(size)
                                    .build();
                            result.add(metric);
                        }
                    } catch (NumberFormatException e) {
                        // queueId가 숫자가 아닌 경우 등 예외 상황을 처리합니다.
                        System.err.println("Invalid key format: " + key);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<Point> points = new ArrayList<>(result.size());
        for (ActionGroupSizeMetric metric : result) {
            var point = this.convertMetricToPoint(metric);
            points.add(point);
        }
        var org = "greenlight";
        var bucket = "metrics";
        influxWriteApi.writePoints(bucket, org, points);

    }

    private Point convertMetricToPoint(ActionGroupSizeMetric metric) {
        return Point.measurement("action_group")
                .addTag("actionGroupId", metric.getActionGroupId().toString())
                .addTag("queueType", metric.getWaitStatus().name())
                .addField("count", metric.getSize());
    }

}