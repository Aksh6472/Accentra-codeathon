package com.accentra.leavemanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class TimeConfig {

    /** Injected everywhere "now"/"today" is needed so time-dependent rules are testable. */
    @Bean
    public Clock clock(AppProperties properties) {
        ZoneId zone = StringUtils.hasText(properties.timeZone())
                ? ZoneId.of(properties.timeZone())
                : ZoneId.systemDefault();
        return Clock.system(zone);
    }
}
