package com.winten.greenlight.scheduler.db.repository.redis.room;

import com.winten.greenlight.scheduler.domain.room.Room;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CachedRoomService {
    private final RoomRepository roomRepository;

    @Cacheable(cacheNames = "roomCache")
    public List<Room> getAllRoomList() {
        return roomRepository.getAllRoomList();
    }

}