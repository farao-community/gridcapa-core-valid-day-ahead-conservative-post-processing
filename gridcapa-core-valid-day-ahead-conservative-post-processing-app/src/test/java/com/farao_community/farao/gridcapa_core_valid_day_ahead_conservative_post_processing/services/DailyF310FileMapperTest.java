/*
 *  Copyright (c) 2025, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.services;

import com.farao_community.farao.gridcapa.task_manager.api.ProcessFileDto;
import com.farao_community.farao.gridcapa.task_manager.api.ProcessFileStatus;
import com.farao_community.farao.gridcapa.task_manager.api.ProcessRunDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskParameterDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative.api.domain.IvaBranchData;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.CoreValidD2PostProcessingConstants;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.fbconstraint.xsd.AdjustmentValueType;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.fbconstraint.xsd.FlowBasedConstraintUpdateDocument;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.fbconstraint.xsd.ReturnedBranchType;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.fbconstraint.xsd.etso.CodingSchemeType;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.fbconstraint.xsd.etso.MessageTypeList;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.fbconstraint.xsd.etso.ProcessTypeList;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.fbconstraint.xsd.etso.RoleTypeList;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DailyF310FileMapperTest {

    private static final ZoneId ZONE_ID = ZoneId.of("CET");

    @Test
    void generateHeaderAtZoneCET() {
        final LocalDate date = LocalDate.of(2025, 12, 1);
        final int testOutputVersion = 3;
        final FlowBasedConstraintUpdateDocument document = new FlowBasedConstraintUpdateDocument();
        DailyF310FileMapper.generateHeader(date, testOutputVersion, document, ZONE_ID);
        Assertions.assertThat(document).isNotNull();
        Assertions.assertThat(document.getDocumentVersion().getV()).isEqualTo(testOutputVersion);
        Assertions.assertThat(document.getDocumentIdentification().getV())
                .isEqualTo(CoreValidD2PostProcessingConstants.XFR_RTE_Q_STRING_VALUE + "-20251201-F310-v3");
        Assertions.assertThat(document.getDocumentType().getV()).isEqualTo(MessageTypeList.B_07);
        Assertions.assertThat(document.getProcessType().getV()).isEqualTo(ProcessTypeList.A_01);
        Assertions.assertThat(document.getSenderIdentification().getV()).isEqualTo(CoreValidD2PostProcessingConstants.XFR_RTE_Q_STRING_VALUE);
        Assertions.assertThat(document.getSenderIdentification().getCodingScheme()).isEqualTo(CodingSchemeType.A_01);
        Assertions.assertThat(document.getSenderRole().getV()).isEqualTo(RoleTypeList.A_04);
        Assertions.assertThat(document.getReceiverIdentification().getCodingScheme()).isEqualTo(CodingSchemeType.A_01);
        Assertions.assertThat(document.getReceiverIdentification().getV()).isEqualTo(CoreValidD2PostProcessingConstants.XTSO_CS_W_STRING_VALUE);
        Assertions.assertThat(document.getReceiverRole().getV()).isEqualTo(RoleTypeList.A_36);
        Assertions.assertThat(document.getConstraintTimeInterval().getV()).isEqualTo("2025-11-30T23:00Z/2025-12-01T23:00Z");
        Assertions.assertThat(document.getDomain().getCodingScheme()).isEqualTo(CodingSchemeType.A_01);
        Assertions.assertThat(document.getDomain().getV()).isEqualTo(CoreValidD2PostProcessingConstants.YFR_RTE_C_STRING_VALUE);
    }

    @Test
    void generateHeaderAtZoneJST() {
        final LocalDate date = LocalDate.of(2025, 12, 1);
        final int testOutputVersion = 2;
        final FlowBasedConstraintUpdateDocument document = new FlowBasedConstraintUpdateDocument();
        DailyF310FileMapper.generateHeader(date, testOutputVersion, document, ZoneId.of("Asia/Tokyo"));
        Assertions.assertThat(document).isNotNull();
        Assertions.assertThat(document.getDocumentVersion().getV()).isEqualTo(testOutputVersion);
        Assertions.assertThat(document.getDocumentIdentification().getV())
                .isEqualTo(CoreValidD2PostProcessingConstants.XFR_RTE_Q_STRING_VALUE + "-20251201-F310-v2");
        Assertions.assertThat(document.getConstraintTimeInterval().getV()).isEqualTo("2025-11-30T15:00Z/2025-12-01T15:00Z");
    }

    @Test
    void generateBodyEmpty() {
        final FlowBasedConstraintUpdateDocument document = new FlowBasedConstraintUpdateDocument();
        DailyF310FileMapper.generateBody(document, Map.of(), ZONE_ID);
        Assertions.assertThat(document.getAdjustmentValues()).isNull();
        Assertions.assertThat(document.getReturnedBranches()).isNull();
    }

    @Test
    void generateBody() {
        final FlowBasedConstraintUpdateDocument document = new FlowBasedConstraintUpdateDocument();
        DailyF310FileMapper.generateBody(document, getTestMap(), ZONE_ID);
        Assertions.assertThat(document.getReturnedBranches()).isNotNull();
        Assertions.assertThat(document.getReturnedBranches().getReturnedBranch())
                .isNotEmpty()
                .hasSize(6)
                .first()
                .isNotNull();
        final ReturnedBranchType firstBranch = document.getReturnedBranches().getReturnedBranch().getFirst();
        Assertions.assertThat(firstBranch.getTimeInterval())
                .hasFieldOrPropertyWithValue("v", "2025-11-28T11:00Z/2025-11-28T12:00Z");
        Assertions.assertThat(firstBranch.isCNEC()).isFalse();
        Assertions.assertThat(firstBranch.getId()).isEqualTo("testId");
        Assertions.assertThat(firstBranch.getReceiverCategory()).isNull();
        Assertions.assertThat(firstBranch.getName()).isEqualTo("testName / testContingency");

        Assertions.assertThat(document.getAdjustmentValues()).isNotNull();
        Assertions.assertThat(document.getAdjustmentValues().getAdjustmentValue())
                .isNotEmpty()
                .hasSize(6)
                .first()
                .isNotNull();
        final AdjustmentValueType firstAdjustementValue = document.getAdjustmentValues().getAdjustmentValue().getFirst();
        Assertions.assertThat(firstAdjustementValue.getTimeInterval())
                .hasFieldOrPropertyWithValue("v", "2025-11-28T11:00Z/2025-11-28T12:00Z");
        Assertions.assertThat(firstAdjustementValue.getCVA()).isNull();
        Assertions.assertThat(firstAdjustementValue.getName()).isEqualTo("testName / testContingency");
        Assertions.assertThat(firstAdjustementValue.getIVA()).isEqualTo(60);
        Assertions.assertThat(firstAdjustementValue.getId()).isEqualTo("testId");
        Assertions.assertThat(firstAdjustementValue.getJustification()).isEqualTo("justification message vertex 2");
        Assertions.assertThat(firstAdjustementValue.getReportingInformation().getCircumstance().getNetpositions().getNp()).hasSize(3);
        Assertions.assertThat(firstAdjustementValue.getReportingInformation().isFallback()).isFalse();
    }

    private Map<TaskDto, List<IvaBranchData>> getTestMap() {
        final Map<TaskDto, List<IvaBranchData>>  testMap = new LinkedHashMap<>();
        testMap.put(getTestTaskDto(OffsetDateTime.of(2025, 11, 28, 12, 0, 0, 0, ZoneOffset.UTC), false),
                    getTestIvaBranches());
        testMap.put(getTestTaskDto(OffsetDateTime.of(2025, 11, 28, 13, 0, 0, 0, ZoneOffset.UTC), false),
                    getTestIvaBranches());
        testMap.put(getTestTaskDto(OffsetDateTime.of(2025, 11, 28, 14, 0, 0, 0, ZoneOffset.UTC), true),
                    getTestIvaBranches());
        return testMap;
    }

    private List<IvaBranchData> getTestIvaBranches() {

        try (InputStream inputStream = getClass().getResource("/testBranchIvaFile.json").openStream()) {
            final ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.reader().forType(new TypeReference<List<IvaBranchData>>() {
            }).readValue(inputStream);
        } catch (IOException e) {
            fail();
        }
        return List.of();
    }

    private static TaskDto getTestTaskDto(final OffsetDateTime timestamp, boolean isOnlyDefaultMessage) {
        return new TaskDto(UUID.randomUUID(),
                           timestamp,
                           TaskStatus.SUCCESS,
                           List.of(),
                           List.of(),
                           List.of(new ProcessFileDto("testFilePath", "IVA-RESULT", ProcessFileStatus.VALIDATED, "tesFileName", "testDocId", timestamp)),
                           List.of(),
                           List.of(new ProcessRunDto(UUID.randomUUID(), timestamp, List.of()),
                                   new ProcessRunDto(UUID.randomUUID(), timestamp, List.of())),
                           List.of(new TaskParameterDto(CoreValidD2PostProcessingConstants.JUSTIFICATION_MESSAGE_ID,
                                                        CoreValidD2PostProcessingConstants.STRING_TYPE,
                                                        isOnlyDefaultMessage ? null : "justification message",
                                                        "defaualt message"))
        );
    }
}
