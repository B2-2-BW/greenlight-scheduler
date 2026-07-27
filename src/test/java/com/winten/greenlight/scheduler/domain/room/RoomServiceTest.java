package com.winten.greenlight.scheduler.domain.room;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoomServiceTest {

    @Test
    void metricCollectorReadsTheBucketAfterItsWriteWindowIsComplete() {
        long now = 10_000L;

        assertEquals(6_000L, RoomService.calculateMetricCounterBucket(now));
        assertEquals(3_000L, RoomService.calculateMetricCollectionBucket(now));
    }
}
