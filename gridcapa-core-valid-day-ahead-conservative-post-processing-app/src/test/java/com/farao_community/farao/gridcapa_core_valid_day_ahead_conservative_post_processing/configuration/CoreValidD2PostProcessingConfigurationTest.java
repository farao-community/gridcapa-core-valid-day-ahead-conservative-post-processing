/*
 *  Copyright (c) 2025, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.configuration;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CoreValidD2PostProcessingConfigurationTest {

    @Autowired
    private CoreValidD2PostProcessingConfiguration coreValidD2PostProcessingConfiguration;

    @Test
    void testConfiguration() {
        Assertions.assertThat(coreValidD2PostProcessingConfiguration)
                .isNotNull();
        Assertions.assertThat(coreValidD2PostProcessingConfiguration.getUrl())
                .isNotNull()
                .hasFieldOrPropertyWithValue("taskManagerTimestampUrl", "http://test-dummy/tasks/")
                .hasFieldOrPropertyWithValue("taskManagerBusinessDateUrl", "http://test-dummy/tasks/businessdate/");
        Assertions.assertThat(coreValidD2PostProcessingConfiguration.getProcess())
                .isNotNull()
                .hasFieldOrPropertyWithValue("tag", "CORE_VALID_D2")
                .hasFieldOrPropertyWithValue("timezone", "CET");
    }
}
