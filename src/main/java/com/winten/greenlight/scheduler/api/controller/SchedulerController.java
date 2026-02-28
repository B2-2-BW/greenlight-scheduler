
package com.winten.greenlight.scheduler.api.controller;

import com.winten.greenlight.scheduler.domain.scheduler.SchedulerConverter;
import com.winten.greenlight.scheduler.domain.scheduler.SchedulerService;
import com.winten.greenlight.scheduler.domain.scheduler.SchedulerStatus;
import com.winten.greenlight.scheduler.domain.scheduler.SchedulerCode;
import jakarta.validation.Valid;
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
        var response = schedulerService.getSchedulerMetaList()
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

    @PutMapping("/{schedulerCode}/delay")
    public ResponseEntity<SchedulerResponse> updateSchedulerDelay(
            @PathVariable SchedulerCode schedulerCode,
            @Valid @RequestBody SchedulerDelayUpdateRequest request
    ) {
        var meta = schedulerService.updateDelay(schedulerCode, request.getDelaySeconds(), request.isRestart());
        var response = schedulerConverter.metaToResponse(meta);
        response.setMessage("Scheduler delay updated");
        return ResponseEntity.ok(response);
    }
}