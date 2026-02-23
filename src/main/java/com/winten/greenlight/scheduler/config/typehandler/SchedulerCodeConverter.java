package com.winten.greenlight.scheduler.config.typehandler;

import com.winten.greenlight.scheduler.domain.scheduler.SchedulerCode;
import org.springframework.core.convert.converter.Converter;

public class SchedulerCodeConverter implements Converter<String, SchedulerCode> {
    @Override
    public SchedulerCode convert(String source) {
        return SchedulerCode.of(source);
    }
}