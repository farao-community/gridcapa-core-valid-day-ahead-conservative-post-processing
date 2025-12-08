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
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.CoreValidD2PostProcessingConstants;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest
class PostProcessingServiceTest {

    @Autowired
    private PostProcessingService postProcessingService;

    @MockitoBean
    private MinioAdapter minioAdapter;

    @Test
    void processTasksOK() {
        final TaskDto taskDto = getTestTaskDto();
        final Set<TaskDto> tasks = Set.of(taskDto);
        try (InputStream in = getClass().getResource("/testBranchIvaFile.json").openStream()) {
            Mockito.when(minioAdapter.getFileFromFullPath("testFilePath")).thenReturn(in);
            postProcessingService.processTasks(LocalDate.of(2025, 11, 28), tasks);
        } catch (IOException e) {
            fail();
        }
    }

    private static TaskDto getTestTaskDto() {
        final OffsetDateTime timestamp = OffsetDateTime.of(2025, 11, 28, 12, 0, 0, 0, ZoneOffset.UTC);
        return new TaskDto(UUID.randomUUID(),
                           timestamp,
                           TaskStatus.SUCCESS,
                           List.of(),
                           List.of(),
                           List.of(new ProcessFileDto("testFilePath", "IVA-RESULT", ProcessFileStatus.VALIDATED, "tesFileName", "testDocId", timestamp)),
                           List.of(),
                           List.of(new ProcessRunDto(UUID.randomUUID(), timestamp, List.of()),
                                   new ProcessRunDto(UUID.randomUUID(), timestamp, List.of())),
                           List.of(new TaskParameterDto("USE_PROJECTION", "BOOLEAN", "true", "true"),
                                   new TaskParameterDto("EXCLUDED_BRANCHES", CoreValidD2PostProcessingConstants.STRING_TYPE, "excluded;branches", "default;excluded;branches"),
                                   new TaskParameterDto(CoreValidD2PostProcessingConstants.JUSTIFICATION_MESSAGE_ID, CoreValidD2PostProcessingConstants.STRING_TYPE, "justification message", "default message"))
                           );
    }
}
