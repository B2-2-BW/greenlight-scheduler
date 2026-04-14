package com.winten.greenlight.scheduler.domain.influx;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApi;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.write.Point;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Service
@RequiredArgsConstructor
public class InfluxService {

    private final InfluxDBClient client;
    private final Queue<InfluxRetryPayload> retryQueue = new ConcurrentLinkedQueue<>();

    // OOM 방지를 위한 최대 큐 사이즈 (예: 최대 10,000번의 Batch 실패까지 보관)
    private static final int MAX_QUEUE_SIZE = 10000;

    @Async("influxTaskExecutor")
    public void writePointsAsync(String bucket, String org, List<Point> points) {
        if (points == null || points.isEmpty()) return;

        WriteApiBlocking writeApi = client.getWriteApiBlocking();

        try {
            // 1. 현재 들어온 데이터 전송 시도
            writeApi.writePoints(bucket, org, points);

            // 2. 현재 데이터 전송에 성공했다면, 과거에 실패해서 큐에 쌓인 데이터들도 재전송 시도
            flushRetryQueue(writeApi);

        } catch (Exception e) {
            // 실패 시 스레드를 블로킹하지 않고 큐에 넣은 뒤 즉시 종료
            log.warn("[InfluxDB] 쓰기 실패. Queue에 임시 저장합니다. (현재 Queue 사이즈: {}) cause: {}",
                    retryQueue.size(), e.getMessage());
            enqueueForRetry(bucket, org, points);
        }
    }

    private void flushRetryQueue(WriteApiBlocking writeApi) {
        // 큐가 비어있지 않다면 반복해서 꺼내어 전송 시도
        while (!retryQueue.isEmpty()) {
            InfluxRetryPayload payload = retryQueue.peek(); // 아직 큐에서 빼지 않고 확인만 함

            try {
                writeApi.writePoints(payload.bucket(), payload.org(), payload.points());
                retryQueue.poll(); // 전송 성공 시 큐에서 제거
                log.info("[InfluxDB] 실패했던 데이터 재전송 완료 (bucket: {})", payload.bucket());
            } catch (Exception e) {
                // 재전송 중 다시 실패하면, 멈추고 다음 스케쥴러 호출을 기약함
                log.warn("[InfluxDB] 재전송 실패. 다음 기회에 다시 시도합니다.");
                break;
            }
        }
    }

    private void enqueueForRetry(String bucket, String org, List<Point> points) {
        if (retryQueue.size() >= MAX_QUEUE_SIZE) {
            // 큐가 가득 차면 가장 오래된 데이터를 버리거나(poll), 현재 데이터를 버리는 정책 선택
            // 여기서는 가장 오래된 데이터를 버리고 새로운 데이터를 넣는 방식 (최신 데이터 유지)
            retryQueue.poll();
            log.error("[InfluxDB] 큐 가득참! 가장 오래된 실패 데이터를 유실합니다.");
        }
        retryQueue.offer(new InfluxRetryPayload(bucket, org, points));
    }
}