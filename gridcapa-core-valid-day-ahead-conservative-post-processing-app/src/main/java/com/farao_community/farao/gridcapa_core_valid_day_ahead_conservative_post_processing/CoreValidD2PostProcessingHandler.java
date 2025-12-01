/*
 *  Copyright (c) 2025, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing;

import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.configuration.CoreValidD2PostProcessingConfiguration;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.services.PostProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

@Service
public class CoreValidD2PostProcessingHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CoreValidD2PostProcessingHandler.class);
    private final CoreValidD2PostProcessingConfiguration coreValidD2PostProcessingConfiguration;
    private final RestTemplateBuilder restTemplateBuilder;
    private final PostProcessingService postProcessingService;

    public CoreValidD2PostProcessingHandler(final CoreValidD2PostProcessingConfiguration coreValidD2PostProcessingConfiguration,
                                            final RestTemplateBuilder restTemplateBuilder,
                                            final PostProcessingService postProcessingService) {
        this.coreValidD2PostProcessingConfiguration = coreValidD2PostProcessingConfiguration;
        this.restTemplateBuilder = restTemplateBuilder;
        this.postProcessingService = postProcessingService;
    }

    /**
     * Trigger postProcessFinishedTasks every time a task is updated
     */
    @Bean
    public Consumer<Flux<TaskDto>> consumeTaskDtoUpdate() {
        return f -> f
            .onErrorContinue((t, r) -> LOGGER.error(t.getMessage(), t))
            .subscribe(this::postProcessFinishedTasks);
    }

    /**
     * Launch processTasks if all tasks associated to localDate are finished
     */
    private void postProcessFinishedTasks(final TaskDto taskDtoUpdated) {
        try {
            if (taskDtoUpdated.getStatus().isOver()) {
                final LocalDate localDate = taskDtoUpdated.getTimestamp().atZoneSameInstant(ZoneId.of("CET")).toLocalDate();
                if (checkIfAllHourlyTasksAreFinished(localDate)) {
                    final Set<TaskDto> allTaskDtoForBusinessDate = getAllTaskDtoForBusinessDate(localDate);
                    // Only perform post processing if a task from local date was updated
                    if (allTaskDtoForBusinessDate.stream().map(TaskDto::getId).anyMatch(uuid -> uuid.equals(taskDtoUpdated.getId()))) {
                        postProcessingService.processTasks(localDate, allTaskDtoForBusinessDate);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * Gather all finished tasks associated to localDate by requesting TaskManager
     * A task is finished when TaskStats::isOver is true
     */
    private boolean checkIfAllHourlyTasksAreFinished(final LocalDate localDate) {
        final String requestUrl = getUrlToCheckAllTasksOfTheDayAreOver(localDate);
        try {
            final ResponseEntity<Boolean> responseEntity = restTemplateBuilder.build().getForEntity(requestUrl, Boolean.class);
            final Boolean body = responseEntity.getBody();
            if (body != null && responseEntity.getStatusCode() == HttpStatus.OK) {
                return body;
            }
        } catch (Exception e) {
            LOGGER.error("Error while checking if all hourly tasks are finished.", e);
        }
        return false;
    }

    /**
     * Gather the set of tasks associated to localDate by requesting TaskManager
     */
    private Set<TaskDto> getAllTaskDtoForBusinessDate(final LocalDate localDate) {
        final String requestUrl = getUrlToGetAllTasksOfTheDay(localDate);
        LOGGER.info("Requesting URL: {}", requestUrl);

        try {
            final ResponseEntity<TaskDto[]> responseEntity = restTemplateBuilder.build().getForEntity(requestUrl, TaskDto[].class);
            if (responseEntity.getBody() != null && responseEntity.getStatusCode() == HttpStatus.OK) {
                return new HashSet<>(Arrays.asList(responseEntity.getBody()));
            }
        } catch (Exception e) {
            LOGGER.error("Error during automatic launch", e);
        }
        LOGGER.warn("Response entity body was null or status was not OK.");
        return Collections.emptySet();
    }

    private String getUrlToCheckAllTasksOfTheDayAreOver(final LocalDate localDate) {
        return coreValidD2PostProcessingConfiguration.getUrl().taskManagerBusinessDateUrl() + localDate + "/allOver";
    }

    private String getUrlToGetAllTasksOfTheDay(final LocalDate localDate) {
        return coreValidD2PostProcessingConfiguration.getUrl().taskManagerBusinessDateUrl() + localDate;
    }
}
