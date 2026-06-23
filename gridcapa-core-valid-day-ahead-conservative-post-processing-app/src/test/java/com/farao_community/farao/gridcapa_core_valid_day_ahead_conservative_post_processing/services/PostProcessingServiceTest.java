/*
 *  Copyright (c) 2025, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.services;

import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.CoreValidD2ConservativeTestUtils;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest
class PostProcessingServiceTest {

    @Autowired
    private PostProcessingService postProcessingService;

    @MockitoBean
    private MinioAdapter minioAdapter;

    @Test
    void processTasksOK() {
        final TaskDto taskDto = CoreValidD2ConservativeTestUtils.getTestTaskDto();
        final Set<TaskDto> tasks = Set.of(taskDto);
        try (InputStream in1 = getClass().getResource("/testBranchIvaFile.json").openStream();
             InputStream in2 = getClass().getResource("/testStudyPointFile.json").openStream()) {
            Mockito.when(minioAdapter.getFileFromFullPath("testIvaFilePath")).thenReturn(in1);
            Mockito.when(minioAdapter.getFileFromFullPath("testNfpFilePath")).thenReturn(in2);
            postProcessingService.processTasks(LocalDate.of(2025, 11, 28), tasks, true);
            Mockito.verify(minioAdapter, Mockito.atLeastOnce()).getFileFromFullPath("testIvaFilePath");
            Mockito.verify(minioAdapter, Mockito.atLeastOnce()).getFileFromFullPath("testNfpFilePath");
        } catch (IOException e) {
            fail("Failed to process tasks due to IOException: " + e.getMessage(), e);
        }
    }

    @Test
    void processTasksOKNoExportStudyPoints() {
        final TaskDto taskDto = CoreValidD2ConservativeTestUtils.getTestTaskDto();
        final Set<TaskDto> tasks = Set.of(taskDto);
        try (InputStream in1 = getClass().getResource("/testBranchIvaFile.json").openStream();
             InputStream in2 = getClass().getResource("/testStudyPointFile.json").openStream()) {
            Mockito.when(minioAdapter.getFileFromFullPath("testIvaFilePath")).thenReturn(in1);
            Mockito.when(minioAdapter.getFileFromFullPath("testNfpFilePath")).thenReturn(in2);
            postProcessingService.processTasks(LocalDate.of(2025, 11, 28), tasks, false);
            Mockito.verify(minioAdapter, Mockito.atLeastOnce()).getFileFromFullPath("testIvaFilePath");
            Mockito.verify(minioAdapter, Mockito.never()).getFileFromFullPath("testNfpFilePath");
        } catch (IOException e) {
            fail("Failed to process tasks due to IOException: " + e.getMessage(), e);
        }
    }
}
