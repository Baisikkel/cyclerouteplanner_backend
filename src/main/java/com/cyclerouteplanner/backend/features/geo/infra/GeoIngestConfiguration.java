package com.cyclerouteplanner.backend.features.geo.infra;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        GeoIngestProperties.class,
        GeoRefreshSchedulerProperties.class,
        GeoRoutingEdgeProperties.class,
        GeoRoutingAutomationProperties.class
})
public class GeoIngestConfiguration {
}
