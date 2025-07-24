package com.winten.greenlight.scheduler.component.factory;

import com.winten.greenlight.scheduler.component.ActionGroupActiveUserSchedulerComponent;
import com.winten.greenlight.scheduler.component.base.AbstractSchedulerComponent;
import com.winten.greenlight.scheduler.component.CustomerRelocationSchedulerComponent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * SchedulerComponentType 에 따라
 * AbstractSchedulerComponent 를 상속한 적합한 SchedulerComponent type return
 * @see SchedulerComponentType
 * @see AbstractSchedulerComponent
 * @see ActionGroupActiveUserSchedulerComponent
 * @see CustomerRelocationSchedulerComponent
 */
@Component
@RequiredArgsConstructor
public class SchedulerComponentFactory {
    private final CustomerRelocationSchedulerComponent customerRelocationSchedulerComponent;
    private final ActionGroupActiveUserSchedulerComponent actionGroupActiveUserSchedulerComponent;

    public AbstractSchedulerComponent getSchedulerComponentBy(SchedulerComponentType type) {
        return switch (type) {
            case RELOCATION -> customerRelocationSchedulerComponent;
            case CAPACITY -> actionGroupActiveUserSchedulerComponent;
            default -> throw new IllegalArgumentException("Unknown scheduler type: [" + type + "]");
        };
    }
}
