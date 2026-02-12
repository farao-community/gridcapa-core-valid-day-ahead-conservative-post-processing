/*
 *  Copyright (c) 2025, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class CoreValidD2PostProcessingConstants {

    public static final String XSD_FILE_NAME = "flowbasedconstraintupdatedocument-14.xsd";
    public static final String OUTPUTS_DIR = "OUTPUTS/";
    public static final ZoneId UTC_ZONE_ID = ZoneId.of("UTC");
    public static final String YFR_RTE_C_STRING_VALUE = "10YFR-RTE------C";
    public static final String XFR_RTE_Q_STRING_VALUE = "10XFR-RTE------Q";
    public static final String XTSO_CS_W_STRING_VALUE = "17XTSO-CS------W";
    public static final String JUSTIFICATION_MESSAGE_ID = "JUSTIFICATION_MESSAGE";
    public static final String STRING_TYPE = "STRING";
    public static final String GENERATED_FILE_PATTERN = "%s-F310-v%s-" + XFR_RTE_Q_STRING_VALUE + "-to-" + XTSO_CS_W_STRING_VALUE + ".xml";
    public static final String VALIDATION_TYPE_COMMENT = "\t<!--validation type 1-->\n";
    public static final String NO_BRANCH_COMMENT = "\t<!--no ReturnedBranch-->\n";
    public static final String NO_ADJUSTMENT_COMMENT = "\t<!--no AdjustmentValue-->\n";
    public static final String DOMAIN_END_HEADER = "<Domain v=\"10YFR-RTE------C\" codingScheme=\"A01\"/>\n";
    public static final String OUTPUT_XML_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mmX";
    public static final String IVA_RESULT = "IVA-RESULT";
    public static final String DOC_ID_PATTERN = "-%s-F310-v%s";
    public static final DateTimeFormatter YYYYMMDD_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private CoreValidD2PostProcessingConstants() {
        throw new IllegalStateException("Constants class");
    }
}
