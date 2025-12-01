/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("core-valid-d2-post-processing")
public class CoreValidD2PostProcessingConfiguration {
    private final UrlProperties url;
    private final ProcessProperties process;

    public CoreValidD2PostProcessingConfiguration(UrlProperties url, ProcessProperties process) {
        this.url = url;
        this.process = process;
    }

    public UrlProperties getUrl() {
        return url;
    }

    public ProcessProperties getProcess() {
        return process;
    }

    public record UrlProperties(String taskManagerTimestampUrl, String taskManagerBusinessDateUrl) {
    }

    public record ProcessProperties(String tag, String timezone) {
    }
}
