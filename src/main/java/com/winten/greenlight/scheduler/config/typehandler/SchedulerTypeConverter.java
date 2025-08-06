package com.winten.greenlight.scheduler.config.typehandler;

import com.winten.greenlight.scheduler.scheduler.factory.SchedulerType;
import org.springframework.core.convert.converter.Converter;

public class SchedulerTypeConverter implements Converter<String, SchedulerType> {
    @Override
    public SchedulerType convert(String source) {
        return SchedulerType.of(source);
    }
}