/*
 *  Copyright (c) 2025, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.services;

import com.farao_community.farao.gridcapa.task_manager.api.ProcessFileDto;
import com.farao_community.farao.gridcapa.task_manager.api.ProcessFileStatus;
import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative.api.domain.IvaBranchData;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.CoreValidD2PostProcessingConstants;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.configuration.CoreValidD2PostProcessingConfiguration;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.exception.CoreValidD2PostProcessingInternalException;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.exception.CoreValidD2PostProcessingInvalidDataException;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.fbconstraint.xsd.FlowBasedConstraintUpdateDocument;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Marshaller;
import org.springframework.stereotype.Service;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.CoreValidD2PostProcessingConstants.IVA_RESULT;
import static com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.CoreValidD2PostProcessingConstants.RETURNED_BRANCHES;

@Service
public class PostProcessingService {

    private final MinioAdapter minioAdapter;
    private final CoreValidD2PostProcessingConfiguration properties;

    public PostProcessingService(final MinioAdapter minioAdapter,
                                 final CoreValidD2PostProcessingConfiguration properties) {
        this.minioAdapter = minioAdapter;
        this.properties = properties;

    }

    public void processTasks(final LocalDate localDate, final Set<TaskDto> tasksToPostProcess) {
        final String outputsTargetMinioFolder = generateTargetMinioFolder(localDate);
        final int outputFileVersion = getOutputFileVersion(tasksToPostProcess);
        final Map<TaskDto, List<IvaBranchData>> ivaResultsPerTask = new HashMap<>();
        final Set<TaskDto> sortedTasksToProcess = tasksToPostProcess.stream()
                .sorted(Comparator.comparing(TaskDto::getTimestamp))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        fillMapOfOutputs(sortedTasksToProcess, ivaResultsPerTask);
        FlowBasedConstraintUpdateDocument constraintUpdateDocument = new FlowBasedConstraintUpdateDocument();
        try {
            final ZoneId zoneId = ZoneId.of(properties.getProcess().timezone());
            DailyF310FileMapper.generateHeader(localDate, outputFileVersion, constraintUpdateDocument, zoneId);
            DailyF310FileMapper.generateBody(constraintUpdateDocument, ivaResultsPerTask, zoneId);
            final String outputFileName = getOutputFileName(localDate, outputFileVersion);
            final byte[] outputFileData = marshallMessageAndSetValidationTypeComment(constraintUpdateDocument);
            try (final InputStream inputStream = new ByteArrayInputStream(outputFileData)) {
                minioAdapter.uploadOutput(outputsTargetMinioFolder + outputFileName, inputStream);
            }
        } catch (IOException e) {
            throw new CoreValidD2PostProcessingInternalException("Could not generate flow based constraint update document file", e);
        }
    }

    private void fillMapOfOutputs(final Set<TaskDto> tasksToProcess,
                                   final Map<TaskDto, List<IvaBranchData>> ivaResults) {
        tasksToProcess.forEach(taskDto ->
                                       taskDto.getOutputs()
                                               .stream()
                                               .filter(processFileDto -> processFileDto.getProcessFileStatus().equals(ProcessFileStatus.VALIDATED))
                                               .forEach(processFileDto -> {
                                                   if (IVA_RESULT.equals(processFileDto.getFileType())) {
                                                       ivaResults.put(taskDto, getIvaResult(processFileDto));
                                                   }
                                               })
        );
    }

    private static int getOutputFileVersion(final Set<TaskDto> tasksToPostProcess) {
        return tasksToPostProcess.stream().mapToInt(task -> task.getRunHistory().size()).max().orElse(1);
    }

    private String generateTargetMinioFolder(final LocalDate localDate) {
        return CoreValidD2PostProcessingConstants.OUTPUTS_DIR + localDate + "/";
    }

    private List<IvaBranchData> getIvaResult(final ProcessFileDto processFileDto) {
        try (final InputStream inputStream = minioAdapter.getFileFromFullPath(processFileDto.getFilePath())) {
            final ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.reader().forType(new TypeReference<List<IvaBranchData>>() {
            }).readValue(inputStream);
        } catch (final IOException e) {
            throw new CoreValidD2PostProcessingInvalidDataException("Error retrieving Iva Result", e);
        }
    }

    private String getOutputFileName(final LocalDate localDate,
                                     final int outputFileVersion) {

        final String date = localDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return String.format(CoreValidD2PostProcessingConstants.GENERATED_FILE_PATTERN, date, outputFileVersion);
    }

    private static byte[] marshallMessageAndSetValidationTypeComment(final FlowBasedConstraintUpdateDocument constraintUpdateDocument) {
        try {
            final StringWriter stringWriter = new StringWriter();
            final JAXBContext jaxbContext = JAXBContext.newInstance(FlowBasedConstraintUpdateDocument.class);
            final Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            final String flowbased = "flowbased";
            final QName qName = new QName(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, flowbased);
            final JAXBElement<FlowBasedConstraintUpdateDocument> root = new JAXBElement<>(qName, FlowBasedConstraintUpdateDocument.class, constraintUpdateDocument);
            jaxbMarshaller.marshal(root, stringWriter);
            return stringWriter.toString()
                    .replace(RETURNED_BRANCHES, CoreValidD2PostProcessingConstants.VALIDATION_TYPE_COMMENT + "\n\t" + RETURNED_BRANCHES)
                    .getBytes();
        } catch (final Exception e) {
            throw new CoreValidD2PostProcessingInternalException("Exception occurred during constraint update document export.", e);
        }
    }
}
