/*
 *  Copyright (c) 2025, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing;

public final class CoreValidD2PostProcessingConstants {

    public static final String OUTPUTS_DIR = "OUTPUTS_DIR/outputs/";
    public static final String UTC_ZONE_ID = "UTC";
    public static final String YFR_RTE_C_STRING_VALUE = "10YFR-RTE------C";
    public static final String XFR_RTE_Q_STRING_VALUE = "10XFR-RTE------Q";
    public static final String XTSO_CS_W_STRING_VALUE = "17XTSO-CS------W";
    public static final String JUSTIFICATION_MESSAGE_ID = "JUSTIFICATION_MESSAGE";
    public static final String STRING_TYPE = "STRING";
    public static final String GENERATED_FILE_PATTERN = "%s-F310-v%s-" + XFR_RTE_Q_STRING_VALUE + "-to-" + XTSO_CS_W_STRING_VALUE + ".xml";
    public static final String VALIDATION_TYPE_COMMENT = "<!--validation type 1-->";
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mmX";
    public static final String RETURNED_BRANCHES = "<ReturnedBranches>";
    public static final String IVA_RESULT = "IVA-RESULT";

    private CoreValidD2PostProcessingConstants() {
        throw new IllegalStateException("Constants class");
    }
}
