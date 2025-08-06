
package com.winten.greenlight.scheduler.api.controller;

import com.winten.greenlight.scheduler.domain.scheduler.SchedulerService;
import com.winten.greenlight.scheduler.domain.scheduler.SchedulerStatus;
import com.winten.greenlight.scheduler.scheduler.factory.SchedulerType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/scheduler")
@RequiredArgsConstructor
public class SchedulerController {

    private final SchedulerService schedulerService;

    @PostMapping("/{schedulerType}/start")
    public ResponseEntity<SchedulerResponse> start(@PathVariable String schedulerType) {
        var type = SchedulerType.from(schedulerType);
        schedulerService.start(type);
        return ResponseEntity.ok(
                SchedulerResponse.builder()
                        .status(SchedulerStatus.RUNNING)
                        .schedulerType(type)
                        .message("Scheduler started")
                        .build()
        );
    }

    @PostMapping("/{schedulerType}/stop")
    public ResponseEntity<SchedulerResponse> stop(@PathVariable String schedulerType) {
        var type = SchedulerType.from(schedulerType);
        schedulerService.stop(type);
        return ResponseEntity.ok(
                SchedulerResponse.builder()
                        .status(SchedulerStatus.STOPPED)
                        .schedulerType(type)
                        .message("Scheduler started")
                        .build()
        );
    }

    @GetMapping("/{schedulerType}/status")
    public ResponseEntity<SchedulerResponse> status(@PathVariable String schedulerType) {
        var type = SchedulerType.from(schedulerType);
        SchedulerStatus status = schedulerService.getStatus(type);
        return ResponseEntity.ok(
                SchedulerResponse.builder()
                        .status(status)
                        .schedulerType(type)
                        .message("Scheduler started")
                        .build()
        );
    }
}