/*
 *  Copyright (c) 2025, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.exception;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class CoreValidD2PostProcessingInvalidDataExceptionTest {
    private static final String TEST_MESSAGE = "testMessage";

    @Test
    void testCreation() {
        final CoreValidD2PostProcessingInvalidDataException exception = new CoreValidD2PostProcessingInvalidDataException(TEST_MESSAGE);
        Assertions.assertThat(exception)
                .isNotNull()
                .hasMessage(TEST_MESSAGE)
                .hasFieldOrPropertyWithValue("title", TEST_MESSAGE)
                .hasFieldOrPropertyWithValue("code", "400-InvalidDataException")
                .hasFieldOrPropertyWithValue("status", 400);
    }

    @Test
    void testCreationWithThrowable() {
        final Throwable throwable = new Throwable();
        final CoreValidD2PostProcessingInvalidDataException exception = new CoreValidD2PostProcessingInvalidDataException(TEST_MESSAGE, throwable);
        Assertions.assertThat(exception)
                .isNotNull()
                .hasMessage(TEST_MESSAGE)
                .hasCause(throwable);
    }
}
