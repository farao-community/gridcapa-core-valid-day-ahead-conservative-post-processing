package com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing;

import com.farao_community.farao.gridcapa.task_manager.api.ProcessFileDto;
import com.farao_community.farao.gridcapa.task_manager.api.ProcessFileStatus;
import com.farao_community.farao.gridcapa.task_manager.api.ProcessRunDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskParameterDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

public final class CoreValidD2ConservativeTestUtils {

    private CoreValidD2ConservativeTestUtils() {
        //Utility class should not be instanciated
        throw new IllegalStateException();
    }

    public static TaskDto getTestTaskDto() {
        return getTestTaskDto(true);
    }

    public static TaskDto getTestTaskDto(final boolean isOver) {
        final OffsetDateTime timestamp = OffsetDateTime.of(2025, 11, 28, 12, 0, 0, 0, ZoneOffset.UTC);
        return getTestTaskDto(isOver, timestamp, true);
    }

    public static TaskDto getTestTaskDto(final OffsetDateTime timestamp, final boolean isOnlyDefaultMessage) {
        return getTestTaskDto(true, timestamp, isOnlyDefaultMessage);
    }

    private static TaskDto getTestTaskDto(final boolean isOver, final OffsetDateTime timestamp, final boolean isOnlyDefaultMessage) {
        return new TaskDto(UUID.randomUUID(),
                           timestamp,
                           isOver ? TaskStatus.SUCCESS : TaskStatus.CREATED,
                           List.of(),
                           List.of(),
                           List.of(new ProcessFileDto("testIvaFilePath", "IVA-RESULT", ProcessFileStatus.VALIDATED, "testFileName", "testDocId", timestamp),
                                   new ProcessFileDto("testNfpFilePath", "STUDY-POINT", ProcessFileStatus.VALIDATED, "testFileName", "testDocId", timestamp)),
                           List.of(),
                           List.of(new ProcessRunDto(UUID.randomUUID(), timestamp, List.of()),
                                   new ProcessRunDto(UUID.randomUUID(), timestamp, List.of())),
                           List.of(new TaskParameterDto(CoreValidD2PostProcessingConstants.JUSTIFICATION_MESSAGE_ID,
                                                        CoreValidD2PostProcessingConstants.STRING_TYPE,
                                                        isOnlyDefaultMessage ? null : "justification message",
                                                        "default message"))
        );
    }

    public static TaskDto getTestTaskDtoNoOutput() {
        final OffsetDateTime timestamp = OffsetDateTime.of(2025, 11, 28, 12, 0, 0, 0, ZoneOffset.UTC);
        return new TaskDto(UUID.randomUUID(),
                           timestamp,
                           TaskStatus.SUCCESS,
                           List.of(),
                           List.of(),
                           List.of(),
                           List.of(),
                           List.of(new ProcessRunDto(UUID.randomUUID(), timestamp, List.of()),
                                   new ProcessRunDto(UUID.randomUUID(), timestamp, List.of())),
                           List.of(new TaskParameterDto(CoreValidD2PostProcessingConstants.JUSTIFICATION_MESSAGE_ID,
                                                        CoreValidD2PostProcessingConstants.STRING_TYPE,
                                                        "justification message",
                                                        "default message"))
        );
    }
}
