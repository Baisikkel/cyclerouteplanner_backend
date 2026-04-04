package com.cyclerouteplanner.backend.core.ratelimit;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RefreshRateLimitProperties.class)
public class RefreshRateLimitConfiguration {
}
