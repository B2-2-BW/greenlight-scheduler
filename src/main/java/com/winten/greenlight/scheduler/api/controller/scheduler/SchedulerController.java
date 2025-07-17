
package com.winten.greenlight.scheduler.api.controller.scheduler;

import com.winten.greenlight.scheduler.domain.actiongroup.service.ActionGroupActiveUserSchedulerService;
import com.winten.greenlight.scheduler.domain.customer.CustomerRelocationSchedulerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/scheduler")
@RequiredArgsConstructor
public class SchedulerController {

    private final CustomerRelocationSchedulerService customerRelocationSchedulerService;
    private final ActionGroupActiveUserSchedulerService actionGroupActiveUserSchedulerService;

    @PostMapping("/relocation/start")
    public ResponseEntity<String> startRelocation() {
        customerRelocationSchedulerService.startScheduler();
        return ResponseEntity.ok("Scheduler started");
    }

    @PostMapping("/relocation/stop")
    public ResponseEntity<String> stopRelocation() {
        customerRelocationSchedulerService.stopScheduler();
        return ResponseEntity.ok("Scheduler stopped");
    }

    @GetMapping("/relocation/status")
    public ResponseEntity<String> statusRelocation() {
        return ResponseEntity.ok(customerRelocationSchedulerService.isRunning() ? "Running" : "Stopped");
    }

    @PostMapping("/available/customers/count/start")
    public ResponseEntity<String> start() {
        actionGroupActiveUserSchedulerService.startScheduler();
        return ResponseEntity.ok("Scheduler started");
    }

    @PostMapping("/available/customers/count/stop")
    public ResponseEntity<String> stop() {
        actionGroupActiveUserSchedulerService.stopScheduler();
        return ResponseEntity.ok("Scheduler stopped");
    }

    @GetMapping("/available/customers/count/status")
    public ResponseEntity<String> status() {
        return ResponseEntity.ok(actionGroupActiveUserSchedulerService.isRunning() ? "Running" : "Stopped");
    }
}
