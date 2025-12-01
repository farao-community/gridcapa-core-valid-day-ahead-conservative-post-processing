/*
 *  Copyright (c) 2025, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.exception;

public class CoreValidD2PostProcessingInternalException extends AbstractCoreValidD2PostProcessingException {
    private static final int STATUS = 500;
    private static final String CODE = "500-InternalException";

    public CoreValidD2PostProcessingInternalException(String message) {
        super(message);
    }

    public CoreValidD2PostProcessingInternalException(String message, Throwable throwable) {
        super(message, throwable);
    }

    @Override
    public int getStatus() {
        return STATUS;
    }

    @Override
    public String getCode() {
        return CODE;
    }
}
