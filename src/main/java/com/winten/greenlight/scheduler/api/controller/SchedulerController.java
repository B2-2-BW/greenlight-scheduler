
package com.winten.greenlight.scheduler.api.controller;

import com.winten.greenlight.scheduler.domain.scheduler.SchedulerService;
import com.winten.greenlight.scheduler.domain.scheduler.SchedulerStatus;
import com.winten.greenlight.scheduler.scheduler.factory.SchedulerType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/schedulers")
@RequiredArgsConstructor
public class SchedulerController {

    private final SchedulerService schedulerService;

    @PostMapping("/{schedulerType}/start")
    public ResponseEntity<SchedulerResponse> start(@PathVariable SchedulerType schedulerType) {
        schedulerService.start(schedulerType);
        return ResponseEntity.ok(
                SchedulerResponse.builder()
                        .status(SchedulerStatus.RUNNING)
                        .schedulerType(schedulerType)
                        .message("Scheduler started")
                        .build()
        );
    }

    @PostMapping("/{schedulerType}/stop")
    public ResponseEntity<SchedulerResponse> stop(@PathVariable SchedulerType schedulerType) {
        schedulerService.stop(schedulerType);
        return ResponseEntity.ok(
                SchedulerResponse.builder()
                        .status(SchedulerStatus.STOPPED)
                        .schedulerType(schedulerType)
                        .message("Scheduler stopped")
                        .build()
        );
    }

    @GetMapping("/status")
    public ResponseEntity<List<SchedulerResponse>> status(@RequestParam(required = false) SchedulerType schedulerType) {
        List<SchedulerResponse> responseList = schedulerService.getStatusList(schedulerType);
        return ResponseEntity.ok(responseList);
    }
}