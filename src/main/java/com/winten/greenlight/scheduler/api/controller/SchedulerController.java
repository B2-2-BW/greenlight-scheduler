
package com.winten.greenlight.scheduler.api.controller;

import com.winten.greenlight.scheduler.domain.scheduler.SchedulerConverter;
import com.winten.greenlight.scheduler.domain.scheduler.SchedulerService;
import com.winten.greenlight.scheduler.domain.scheduler.SchedulerStatus;
import com.winten.greenlight.scheduler.domain.scheduler.SchedulerCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/schedulers")
@RequiredArgsConstructor
public class SchedulerController {
    private final SchedulerService schedulerService;
    private final SchedulerConverter schedulerConverter;

    @GetMapping("")
    public ResponseEntity<List<SchedulerResponse>> getAllSchedulers() {
        var response = schedulerService.getSchedulersMeta()
                .stream()
                .map(schedulerConverter::metaToResponse)
                .toList();
        return ResponseEntity.ok(response);
    }


    @PostMapping("/{schedulerCode}/start")
    public ResponseEntity<SchedulerResponse> start(@PathVariable SchedulerCode schedulerCode) {
        schedulerService.start(schedulerCode);
        return ResponseEntity.ok(
                SchedulerResponse.builder()
                        .status(SchedulerStatus.RUNNING)
                        .schedulerCode(schedulerCode)
                        .message("Scheduler started")
                        .build()
        );
    }

    @PostMapping("/{schedulerCode}/stop")
    public ResponseEntity<SchedulerResponse> stop(@PathVariable SchedulerCode schedulerCode) {
        schedulerService.stop(schedulerCode);
        return ResponseEntity.ok(
                SchedulerResponse.builder()
                        .status(SchedulerStatus.STOPPED)
                        .schedulerCode(schedulerCode)
                        .message("Scheduler stopped")
                        .build()
        );
    }

    @GetMapping("/status")
    public ResponseEntity<List<SchedulerResponse>> status(@RequestParam(required = false) SchedulerCode schedulerCode) {
        List<SchedulerResponse> responseList = schedulerService.getStatusList(schedulerCode);
        return ResponseEntity.ok(responseList);
    }
}