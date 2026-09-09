package com.winten.greenlight.scheduler.domain.room;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomServiceTest {

    @Test
    void metricCollectorReadsTheBucketAfterItsWriteWindowIsComplete() {
        long now = 10_000L;

        assertEquals(6_000L, RoomService.calculateMetricCounterBucket(now));
        assertEquals(3_000L, RoomService.calculateMetricCollectionBucket(now));
    }

    @Test
    void moveTicketChunkIsCappedBelowLuaUnpackLimit() {
        assertEquals(1_000, RoomService.MOVE_TICKET_CHUNK_SIZE);
        assertTrue(1 + 2 * RoomService.MOVE_TICKET_CHUNK_SIZE < 8000);
    }
}
