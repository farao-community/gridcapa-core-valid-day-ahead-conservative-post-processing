/*
 *  Copyright (c) 2025, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing;

import com.farao_community.farao.gridcapa.task_manager.api.ProcessFileDto;
import com.farao_community.farao.gridcapa.task_manager.api.ProcessFileStatus;
import com.farao_community.farao.gridcapa.task_manager.api.ProcessRunDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskParameterDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CoreValidD2PostProcessingHandlerTest {

    @Autowired
    private CoreValidD2PostProcessingHandler coreValidD2PostProcessingHandler;

    @MockitoBean
    private RestTemplateBuilder restTemplateBuilder;

    @MockitoBean
    MinioAdapter minioAdapter;

    @Test
    void consumeTaskDtoUpdate() {
        TaskDto taskDto = getTestTaskDto();
        TaskDto[] tasks = {taskDto};
        Flux<TaskDto> taskDtoFlux = Flux.fromStream(Stream.of(taskDto));
        Consumer<Flux<TaskDto>> consumer = coreValidD2PostProcessingHandler.consumeTaskDtoUpdate();
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        ResponseEntity response1 = Mockito.mock(ResponseEntity.class);
        Mockito.when(response1.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(response1.getBody()).thenReturn(Boolean.TRUE);
        ResponseEntity response2 = Mockito.mock(ResponseEntity.class);
        Mockito.when(response2.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(response2.getBody()).thenReturn(tasks);
        Mockito.when(restTemplate.getForEntity(Mockito.eq("http://test-dummy/tasks/businessdate/2025-11-28/allOver"), Mockito.eq(Boolean.class))).thenReturn(response1);
        Mockito.when(restTemplate.getForEntity(Mockito.eq("http://test-dummy/tasks/businessdate/2025-11-28"), Mockito.eq(TaskDto[].class))).thenReturn(response2);
        Mockito.when(restTemplateBuilder.build()).thenReturn(restTemplate);
        try (InputStream in = getClass().getResource("/testBranchIvaFile.json").openStream()) {
            Mockito.when(minioAdapter.getFileFromFullPath(Mockito.eq("testFilePath"))).thenReturn(in);
            consumer.accept(taskDtoFlux);
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
                           List.of(new TaskParameterDto(CoreValidD2PostProcessingConstants.JUSTIFICATION_MESSAGE_ID, CoreValidD2PostProcessingConstants.STRING_TYPE, "justification message", "defaualt message"))
                           );
    }
}
