package com.winten.greenlight.scheduler.domain.room;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class Room {
    private String roomId;
    private String siteId;
    private String name;
    private String description;
    private Integer maxTrafficPerSecond;
    private Integer capacity;
    private Boolean enabled;
    private String defaultDestinationUrl;
    private boolean updateRule;
}