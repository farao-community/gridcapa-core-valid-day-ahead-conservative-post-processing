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

import javax.xml.namespace.QName;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.CoreValidD2PostProcessingConstants.DOMAIN_END_HEADER;
import static com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.CoreValidD2PostProcessingConstants.IVA_RESULT;
import static com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.CoreValidD2PostProcessingConstants.NO_ADJUSTMENT_COMMENT;
import static com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.CoreValidD2PostProcessingConstants.NO_BRANCH_COMMENT;
import static com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.CoreValidD2PostProcessingConstants.VALIDATION_TYPE_COMMENT;
import static com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.CoreValidD2PostProcessingConstants.XSD_FILE_NAME;

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
        final int outputFileVersion = getOutputFileVersion(tasksToPostProcess);
        final Map<TaskDto, List<IvaBranchData>> ivaResultsPerTask = new TreeMap<>(Comparator.comparing(TaskDto::getTimestamp));
        fillMapOfOutputs(tasksToPostProcess, ivaResultsPerTask);
        final FlowBasedConstraintUpdateDocument constraintUpdateDocument = new FlowBasedConstraintUpdateDocument();
        constraintUpdateDocument.setDtdRelease("4");
        constraintUpdateDocument.setDtdVersion("0");
        try {
            final ZoneId zoneId = ZoneId.of(properties.getProcess().timezone());
            DailyF310FileMapper.generateHeader(localDate, outputFileVersion, constraintUpdateDocument, zoneId);
            DailyF310FileMapper.generateBody(constraintUpdateDocument, ivaResultsPerTask);
            final String outputFileName = getOutputFileName(localDate, outputFileVersion);
            final byte[] outputFileData = marshallMessageAndSetValidationTypeComment(constraintUpdateDocument);
            try (final InputStream inputStream = new ByteArrayInputStream(outputFileData)) {
                minioAdapter.uploadOutput(CoreValidD2PostProcessingConstants.OUTPUTS_DIR + outputFileName, inputStream);
            }
        } catch (final IOException e) {
            throw new CoreValidD2PostProcessingInternalException("Could not generate flow based constraint update document file", e);
        }
    }

    private void fillMapOfOutputs(final Set<TaskDto> tasksToProcess,
                                  final Map<TaskDto, List<IvaBranchData>> ivaResults) {
        tasksToProcess.forEach(taskDto ->
                                       taskDto.getOutputs()
                                               .stream()
                                               .filter(processFileDto -> processFileDto.getProcessFileStatus() == ProcessFileStatus.VALIDATED
                                                                         && IVA_RESULT.equals(processFileDto.getFileType()))
                                               .forEach(processFileDto -> ivaResults.put(taskDto, getIvaResult(processFileDto)))
        );
    }

    private static int getOutputFileVersion(final Set<TaskDto> tasksToPostProcess) {
        return tasksToPostProcess.stream().mapToInt(task -> task.getRunHistory().size()).max().orElse(1);
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

    private byte[] marshallMessageAndSetValidationTypeComment(final FlowBasedConstraintUpdateDocument constraintUpdateDocument) {
        try {
            final StringWriter stringWriter = new StringWriter();
            final JAXBContext jaxbContext = JAXBContext.newInstance(FlowBasedConstraintUpdateDocument.class);
            final Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, XSD_FILE_NAME);
            final QName elementName = jaxbContext.createJAXBIntrospector().getElementName(constraintUpdateDocument);
            final boolean hasAdjustmentValues =  constraintUpdateDocument.getAdjustmentValues() != null;
            final JAXBElement<FlowBasedConstraintUpdateDocument> root = new JAXBElement<>(elementName, FlowBasedConstraintUpdateDocument.class, constraintUpdateDocument);
            jaxbMarshaller.marshal(root, stringWriter);
            final String fileXmlString = stringWriter.toString();
            final String commentedXML = fileXmlString.replace(DOMAIN_END_HEADER, DOMAIN_END_HEADER
                                                                                 + VALIDATION_TYPE_COMMENT
                                                                                 + NO_BRANCH_COMMENT
                                                                                 + (hasAdjustmentValues ? "" : NO_ADJUSTMENT_COMMENT));
            return commentedXML.getBytes(StandardCharsets.UTF_8);
        } catch (final Exception e) {
            throw new CoreValidD2PostProcessingInternalException("Exception occurred during constraint update document export.", e);
        }
    }
}
