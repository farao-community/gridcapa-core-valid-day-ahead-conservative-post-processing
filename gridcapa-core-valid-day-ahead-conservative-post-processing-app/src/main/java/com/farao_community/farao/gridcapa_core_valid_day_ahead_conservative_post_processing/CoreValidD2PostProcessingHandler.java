/*
 *  Copyright (c) 2025, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing;

import com.farao_community.farao.gridcapa.task_manager.api.ProcessFileStatus;
import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
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
import java.util.concurrent.TimeUnit;
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
                final ZoneId zoneId = ZoneId.of(coreValidD2PostProcessingConfiguration.process().timezone());
                final LocalDate localDate = taskDtoUpdated.getTimestamp().atZoneSameInstant(zoneId).toLocalDate();
                if (areAllHourlyTasksFinished(localDate)) {
                    final Set<TaskDto> allTaskDtoForBusinessDate = getAllTaskDtoForBusinessDate(localDate);
                    // Only perform post processing if a task from local date was updated
                    final boolean anyTasksHaveBeenUpdated = allTaskDtoForBusinessDate.stream().map(TaskDto::getId).anyMatch(uuid -> uuid.equals(taskDtoUpdated.getId()));
                    if (anyTasksHaveBeenUpdated) {
                        postProcessingService.processTasks(localDate, allTaskDtoForBusinessDate);
                    }
                }
            }
        } catch (final Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * Gather all finished tasks associated to localDate by requesting TaskManager
     * A task is finished when TaskStats::isOver is true
     */
    private boolean areAllHourlyTasksFinished(final LocalDate localDate) {
        final String requestUrl = getUrlToCheckIfAllTasksOfTheDayAreOver(localDate);
        try {
            final ResponseEntity<Boolean> responseEntity = restTemplateBuilder.build().getForEntity(requestUrl, Boolean.class);
            final Boolean body = responseEntity.getBody();
            return body != null && responseEntity.getStatusCode() == HttpStatus.OK && body;
        } catch (final Exception e) {
            LOGGER.error("Error while checking if all hourly tasks are finished.", e);
        }
        return false;
    }

    /**
     * Gather the set of tasks associated to localDate by requesting TaskManager
     * And retry until all finished tasks have outputs
     */
    private Set<TaskDto> getAllTaskDtoForBusinessDate(final LocalDate localDate) {
        final String requestUrl = getUrlToGetAllTasksOfTheDay(localDate);
        LOGGER.info("Requesting URL: {}", requestUrl);
        final int maxRetryCount = coreValidD2PostProcessingConfiguration.process().fetchTaskManagerRetryCount();
        final int retryWaitPeriod = coreValidD2PostProcessingConfiguration.process().fetchTaskManagerRetryWait();
        int retrycounter = 0;
        boolean allOutputsAvailable;
        try {
            do {
                final ResponseEntity<TaskDto[]> responseEntity = restTemplateBuilder.build().getForEntity(requestUrl, TaskDto[].class);
                if (responseEntity.getBody() != null && responseEntity.getStatusCode() == HttpStatus.OK) {
                    final Set<TaskDto> allTasks = new HashSet<>(Arrays.asList(responseEntity.getBody()));
                    allOutputsAvailable = allTasks.stream()
                            .filter(task -> task.getStatus() == TaskStatus.SUCCESS)
                            .allMatch(this::checkAllOutputFilesValidated);
                    if (allOutputsAvailable) {
                        return allTasks;
                    }
                }
                TimeUnit.SECONDS.sleep(retryWaitPeriod);
            } while (retrycounter++ < maxRetryCount);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        } catch (final Exception e) {
            LOGGER.error("Error during automatic launch", e);
        }
        LOGGER.warn("Response entity body was null or status was not OK.");
        return Collections.emptySet();
    }

    private String getUrlToCheckIfAllTasksOfTheDayAreOver(final LocalDate localDate) {
        return coreValidD2PostProcessingConfiguration.url().taskManagerBusinessDateUrl() + localDate + "/allOver";
    }

    private String getUrlToGetAllTasksOfTheDay(final LocalDate localDate) {
        return coreValidD2PostProcessingConfiguration.url().taskManagerBusinessDateUrl() + localDate;
    }

    private boolean checkAllOutputFilesValidated(final TaskDto taskDtoUpdated) {
        return taskDtoUpdated.getOutputs().stream().allMatch(output -> output.getProcessFileStatus().equals(ProcessFileStatus.VALIDATED));
    }

}
