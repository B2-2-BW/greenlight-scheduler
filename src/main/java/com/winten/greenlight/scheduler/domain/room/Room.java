package com.winten.greenlight.scheduler.domain.room;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class Room {
    private String roomId;
    private String siteId;
    private RoomEnvironment roomEnvironment;
    private String name;
    private String description;
    private Integer maxTrafficPerSecond;
    private Integer capacity;
    private Boolean enabled;
    private String defaultDestinationUrl;
    private boolean updateRule;
}