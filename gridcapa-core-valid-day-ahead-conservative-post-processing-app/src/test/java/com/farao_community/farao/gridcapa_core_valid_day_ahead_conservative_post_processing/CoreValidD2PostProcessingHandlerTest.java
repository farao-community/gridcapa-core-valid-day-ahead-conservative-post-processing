/*
 *  Copyright (c) 2025, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing;

import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.services.PostProcessingService;
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

import java.time.LocalDate;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

@SpringBootTest
class CoreValidD2PostProcessingHandlerTest {

    @Autowired
    private CoreValidD2PostProcessingHandler coreValidD2PostProcessingHandler;

    @MockitoBean
    private RestTemplateBuilder restTemplateBuilder;

    @MockitoBean
    private MinioAdapter minioAdapter;

    @MockitoBean
    private PostProcessingService postProcessingService;

    @Test
    void consumeTaskDtoUpdateOK() {
        Mockito.reset(postProcessingService);
        final TaskDto taskDto = CoreValidD2ConservativeTestUtils.getTestTaskDto(true);
        final TaskDto[] tasks = {taskDto};
        final Flux<TaskDto> taskDtoFlux = Flux.fromStream(Stream.of(taskDto));
        final Consumer<Flux<TaskDto>> consumer = coreValidD2PostProcessingHandler.consumeTaskDtoUpdate();
        final RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        final ResponseEntity response1 = Mockito.mock(ResponseEntity.class);
        Mockito.when(response1.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(response1.getBody()).thenReturn(Boolean.TRUE);
        final ResponseEntity response2 = Mockito.mock(ResponseEntity.class);
        Mockito.when(response2.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(response2.getBody()).thenReturn(tasks);
        Mockito.when(restTemplate.getForEntity("http://test-dummy/tasks/businessdate/2025-11-28/allOver", Boolean.class)).thenReturn(response1);
        Mockito.when(restTemplate.getForEntity("http://test-dummy/tasks/businessdate/2025-11-28", TaskDto[].class)).thenReturn(response2);
        Mockito.when(restTemplateBuilder.build()).thenReturn(restTemplate);
        consumer.accept(taskDtoFlux);
        Mockito.verify(postProcessingService, Mockito.atLeastOnce()).processTasks(Mockito.any(LocalDate.class), Mockito.any());
    }

    @Test
    void consumeTaskDtoUpdateNotAllFinished() {
        Mockito.reset(postProcessingService);
        final TaskDto taskDto = CoreValidD2ConservativeTestUtils.getTestTaskDto(true);
        final Flux<TaskDto> taskDtoFlux = Flux.fromStream(Stream.of(taskDto));
        final Consumer<Flux<TaskDto>> consumer = coreValidD2PostProcessingHandler.consumeTaskDtoUpdate();
        final RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        final ResponseEntity response1 = Mockito.mock(ResponseEntity.class);
        Mockito.when(response1.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(response1.getBody()).thenReturn(Boolean.FALSE);
        Mockito.when(restTemplate.getForEntity("http://test-dummy/tasks/businessdate/2025-11-28/allOver", Boolean.class)).thenReturn(response1);
        consumer.accept(taskDtoFlux);
        Mockito.verify(postProcessingService, Mockito.never()).processTasks(Mockito.any(LocalDate.class), Mockito.any());
    }

    @Test
    void consumeTaskDtoUpdateNotFinished() {
        Mockito.reset(postProcessingService);
        final TaskDto taskDto = CoreValidD2ConservativeTestUtils.getTestTaskDto(false);
        final Flux<TaskDto> taskDtoFlux = Flux.fromStream(Stream.of(taskDto));
        final Consumer<Flux<TaskDto>> consumer = coreValidD2PostProcessingHandler.consumeTaskDtoUpdate();
        consumer.accept(taskDtoFlux);
        Mockito.verify(postProcessingService, Mockito.never()).processTasks(Mockito.any(LocalDate.class), Mockito.any());
    }

    @Test
    void consumeTaskDtoUpdateOKButNoFiles() {
        Mockito.reset(postProcessingService);
        final TaskDto taskDto = CoreValidD2ConservativeTestUtils.getTestTaskDtoNoOutput();
        final TaskDto[] tasks = {taskDto};
        final Flux<TaskDto> taskDtoFlux = Flux.fromStream(Stream.of(taskDto));
        final Consumer<Flux<TaskDto>> consumer = coreValidD2PostProcessingHandler.consumeTaskDtoUpdate();
        final RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        final ResponseEntity response1 = Mockito.mock(ResponseEntity.class);
        Mockito.when(response1.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(response1.getBody()).thenReturn(Boolean.TRUE);
        final ResponseEntity response2 = Mockito.mock(ResponseEntity.class);
        Mockito.when(response2.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(response2.getBody()).thenReturn(tasks);
        Mockito.when(restTemplate.getForEntity("http://test-dummy/tasks/businessdate/2025-11-28/allOver", Boolean.class)).thenReturn(response1);
        Mockito.when(restTemplate.getForEntity("http://test-dummy/tasks/businessdate/2025-11-28", TaskDto[].class)).thenReturn(response2);
        Mockito.when(restTemplateBuilder.build()).thenReturn(restTemplate);
        consumer.accept(taskDtoFlux);
        Mockito.verify(postProcessingService, Mockito.atMostOnce()).processTasks(Mockito.any(LocalDate.class), Mockito.eq(Set.of()));
    }

    @Test
    void consumeTaskDtoUpdateNotOK() {
        Mockito.reset(postProcessingService);
        final TaskDto taskDto = CoreValidD2ConservativeTestUtils.getTestTaskDtoNoOutput();
        final TaskDto[] tasks = {taskDto};
        final Flux<TaskDto> taskDtoFlux = Flux.fromStream(Stream.of(taskDto));
        final Consumer<Flux<TaskDto>> consumer = coreValidD2PostProcessingHandler.consumeTaskDtoUpdate();
        final RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        final ResponseEntity response1 = Mockito.mock(ResponseEntity.class);
        Mockito.when(response1.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(response1.getBody()).thenReturn(Boolean.TRUE);
        final ResponseEntity response2 = Mockito.mock(ResponseEntity.class);
        Mockito.when(response2.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
        Mockito.when(response2.getBody()).thenReturn(tasks);
        Mockito.when(restTemplate.getForEntity("http://test-dummy/tasks/businessdate/2025-11-28/allOver", Boolean.class)).thenReturn(response1);
        Mockito.when(restTemplate.getForEntity("http://test-dummy/tasks/businessdate/2025-11-28", TaskDto[].class)).thenReturn(response2);
        Mockito.when(restTemplateBuilder.build()).thenReturn(restTemplate);
        consumer.accept(taskDtoFlux);
        Mockito.verify(postProcessingService, Mockito.atMostOnce()).processTasks(Mockito.any(LocalDate.class), Mockito.eq(Set.of()));
    }
}
