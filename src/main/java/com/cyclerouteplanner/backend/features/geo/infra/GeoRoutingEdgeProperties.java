package com.cyclerouteplanner.backend.features.geo.infra;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "geo.routing-edges")
public class GeoRoutingEdgeProperties {

    private String exportOutputPath;
    private String exportPseudoTagsOutputPath;

    public String getExportOutputPath() {
        return exportOutputPath;
    }

    public void setExportOutputPath(String exportOutputPath) {
        this.exportOutputPath = exportOutputPath;
    }

    public String getExportPseudoTagsOutputPath() {
        return exportPseudoTagsOutputPath;
    }

    public void setExportPseudoTagsOutputPath(String exportPseudoTagsOutputPath) {
        this.exportPseudoTagsOutputPath = exportPseudoTagsOutputPath;
    }
}
