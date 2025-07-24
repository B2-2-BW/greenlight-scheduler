
package com.winten.greenlight.scheduler.api.controller;

import com.winten.greenlight.scheduler.component.factory.SchedulerComponentFactory;
import com.winten.greenlight.scheduler.component.factory.SchedulerComponentType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/scheduler")
@RequiredArgsConstructor
public class SchedulerController {

    private final SchedulerComponentFactory schedulerComponentFactory;

    @PostMapping("/{schedulerType}/start")
    public ResponseEntity<String> start(@PathVariable String schedulerType) {
        schedulerComponentFactory.getSchedulerComponentBy(SchedulerComponentType.from(schedulerType)).start();
        return ResponseEntity.ok("[" + schedulerType + "] scheduler started");
    }

    @PostMapping("/{schedulerType}/stop")
    public ResponseEntity<String> stop(@PathVariable String schedulerType) {
        schedulerComponentFactory.getSchedulerComponentBy(SchedulerComponentType.from(schedulerType)).stop();
        return ResponseEntity.ok("[" + schedulerType + "] scheduler stopped");
    }

    @GetMapping("/{schedulerType}/status")
    public ResponseEntity<String> status(@PathVariable String schedulerType) {
        boolean isRunning = schedulerComponentFactory.getSchedulerComponentBy(SchedulerComponentType.from(schedulerType)).isRunning();
        return ResponseEntity.ok("[" + schedulerType + "] scheduler is " + (isRunning ? "Running" : "Stopped"));
    }
}
