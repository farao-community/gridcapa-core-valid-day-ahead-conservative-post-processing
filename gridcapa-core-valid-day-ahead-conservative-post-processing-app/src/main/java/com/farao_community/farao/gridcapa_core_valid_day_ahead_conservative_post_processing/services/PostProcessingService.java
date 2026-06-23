/*
 *  Copyright (c) 2025, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.services;

import com.farao_community.farao.gridcapa.task_manager.api.ProcessFileDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import com.farao_community.farao.gridcapa_core_valid_commons.vertex.Vertex;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative.api.domain.IvaBranchData;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative.api.domain.StudyPoint;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.configuration.CoreValidD2PostProcessingConfiguration;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.exception.CoreValidD2PostProcessingInternalException;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.exception.CoreValidD2PostProcessingInvalidDataException;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.fbconstraint.xsd.FlowBasedConstraintUpdateDocument;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import javax.xml.namespace.QName;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;

import static com.farao_community.farao.gridcapa.task_manager.api.ProcessFileStatus.VALIDATED;
import static com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.CoreValidD2PostProcessingConstants.DOMAIN_END_HEADER;
import static com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.CoreValidD2PostProcessingConstants.IVA_GENERATED_FILE_PATTERN;
import static com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.CoreValidD2PostProcessingConstants.IVA_RESULT;
import static com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.CoreValidD2PostProcessingConstants.NO_ADJUSTMENT_COMMENT;
import static com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.CoreValidD2PostProcessingConstants.NO_BRANCH_COMMENT;
import static com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.CoreValidD2PostProcessingConstants.OUTPUTS_DIR;
import static com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.CoreValidD2PostProcessingConstants.OUTPUT_XML_RELEASE;
import static com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.CoreValidD2PostProcessingConstants.OUTPUT_XML_VERSION;
import static com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.CoreValidD2PostProcessingConstants.STUDY_POINTS;
import static com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.CoreValidD2PostProcessingConstants.STUDY_POINTS_HEADER_PREFIX;
import static com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.CoreValidD2PostProcessingConstants.STUDY_POINT_GENERATED_FILE_PATTERN;
import static com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.CoreValidD2PostProcessingConstants.VALIDATION_TYPE_COMMENT;
import static com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.CoreValidD2PostProcessingConstants.XSD_FILE_NAME;
import static com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.CoreValidD2PostProcessingConstants.YYYYMMDD_FORMATTER;
import static com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.services.DailyF310FileMapper.generateBody;
import static com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.services.DailyF310FileMapper.generateHeader;
import static jakarta.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT;
import static jakarta.xml.bind.Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Comparator.comparing;

@Service
public class PostProcessingService {

    private static final JAXBContext JAXB_CONTEXT = initJaxbContext();

    private final MinioAdapter minioAdapter;
    private final CoreValidD2PostProcessingConfiguration properties;

    public PostProcessingService(final MinioAdapter minioAdapter,
                                 final CoreValidD2PostProcessingConfiguration properties) {
        this.minioAdapter = minioAdapter;
        this.properties = properties;
    }

    public void processTasks(final LocalDate localDate,
                             final Set<TaskDto> tasksToPostProcess,
                             final boolean exportStudyPoints) {
        final int outputFileVersion = getOutputFileVersion(tasksToPostProcess);
        final Map<TaskDto, List<IvaBranchData>> ivaResultsPerTask = new TreeMap<>(comparing(TaskDto::getTimestamp));
        final Map<TaskDto, List<StudyPoint>> studyPointsPerTask = new TreeMap<>(comparing(TaskDto::getTimestamp));
        fillMapOfOutputs(tasksToPostProcess, ivaResultsPerTask, studyPointsPerTask, exportStudyPoints);
        exportIvaResult(localDate, outputFileVersion, ivaResultsPerTask);
        if (exportStudyPoints) {
            exportStudyPointResult(localDate, outputFileVersion, studyPointsPerTask);
        }
    }

    private void exportStudyPointResult(final LocalDate localDate,
                                        final int outputFileVersion,
                                        final Map<TaskDto, List<StudyPoint>> studyPointsPerTask) {
        // we need the keys for the header
        final List<String> npKeys = getListofSortedNpKeys(studyPointsPerTask);
        final List<String> headerNpKeys = getHeaderNpKeys(npKeys);
        final List<String> header = new ArrayList<>();
        header.add("Periode");
        header.add("ID");
        header.addAll(headerNpKeys);
        final CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(header.toArray(new String[0]))
                .setDelimiter(";")
                .build();
        try (final StringWriter stringWriter = new StringWriter();
             final CSVPrinter csvPrinter = new CSVPrinter(stringWriter, csvFormat)) {
            studyPointsPerTask.forEach((taskDto, studyPoints) -> {
                studyPoints.forEach(studyPoint -> {
                    final int position = studyPoint.position();
                    myCsvPrint(csvPrinter, position);
                    final Vertex vertex = studyPoint.vertex();
                    final String id = position + "_" + vertex.vertexId();
                    myCsvPrint(csvPrinter, id);
                    final Map<String, Integer> nps = vertex.coordinates();
                    npKeys.forEach(key -> myCsvPrint(csvPrinter, nps.get(key)));
                    myCsvPrintln(csvPrinter);
                });
            });
            csvPrinter.flush();
            minioAdapter.uploadOutput(OUTPUTS_DIR + getOutputFileName(STUDY_POINT_GENERATED_FILE_PATTERN, localDate, outputFileVersion),
                                      new ByteArrayInputStream(stringWriter.toString().getBytes()));
        } catch (Exception e) {
            throw new CoreValidD2PostProcessingInternalException("Could not generate study point document file", e);
        }
    }

    private List<String> getHeaderNpKeys(final List<String> npKeys) {
        return npKeys.stream()
                .map(s -> STUDY_POINTS_HEADER_PREFIX + s)
                .toList();
    }

    private static @NotNull List<String> getListofSortedNpKeys(final Map<TaskDto, List<StudyPoint>> studyPointsPerTask) {
        return studyPointsPerTask.values().stream()
                .findFirst()
                .orElseThrow(getCoreValidD2InternalExceptionSupplier())
                .stream().findFirst()
                .orElseThrow(getCoreValidD2InternalExceptionSupplier())
                .vertex()
                .coordinates()
                .keySet()
                .stream()
                .sorted()
                .toList();
    }

    private static @NotNull Supplier<CoreValidD2PostProcessingInternalException> getCoreValidD2InternalExceptionSupplier() {
        return () -> new CoreValidD2PostProcessingInternalException("Could not generate study point document file: no study points");
    }

    private void myCsvPrint(CSVPrinter printer, Object toPrint) {
        try {
            printer.print(toPrint);
        } catch (Exception e) {
            throw new CoreValidD2PostProcessingInternalException("Error printing CSV study point document element: " + toPrint, e);
        }
    }

    private void myCsvPrintln(CSVPrinter printer) {
        try {
            printer.println();
        } catch (Exception e) {
            throw new CoreValidD2PostProcessingInternalException("Error printing CSV study point println", e);
        }
    }

    private void exportIvaResult(final LocalDate localDate,
                                 final int outputFileVersion,
                                 final Map<TaskDto, List<IvaBranchData>> ivaResultsPerTask) {
        final FlowBasedConstraintUpdateDocument fbCtUpdateDoc = new FlowBasedConstraintUpdateDocument();
        fbCtUpdateDoc.setDtdRelease(OUTPUT_XML_RELEASE);
        fbCtUpdateDoc.setDtdVersion(OUTPUT_XML_VERSION);

        try {
            final ZoneId zoneId = ZoneId.of(properties.process().timezone());
            generateHeader(localDate, outputFileVersion, fbCtUpdateDoc, zoneId);
            generateBody(fbCtUpdateDoc, ivaResultsPerTask);

            final byte[] outputFileData = marshallMessageAndSetValidationTypeComment(fbCtUpdateDoc);
            minioAdapter.uploadOutput(OUTPUTS_DIR + getOutputFileName(IVA_GENERATED_FILE_PATTERN, localDate, outputFileVersion),
                                      new ByteArrayInputStream(outputFileData));

        } catch (final Exception e) {
            throw new CoreValidD2PostProcessingInternalException("Could not generate flow based constraint update document file", e);
        }
    }

    private void fillMapOfOutputs(final Set<TaskDto> tasksToProcess,
                                  final Map<TaskDto, List<IvaBranchData>> ivaResults,
                                  final Map<TaskDto, List<StudyPoint>> studyPointsPerTask,
                                  final boolean exportStudyPoints) {
        tasksToProcess.forEach(task ->
                                   task.getOutputs()
                                       .stream()
                                       .filter(file -> fileFilterByType(file, IVA_RESULT))
                                       .map(this::getIvaResult)
                                       .forEach(iva -> ivaResults.put(task, iva))
        );
        if (exportStudyPoints) {
            tasksToProcess.forEach(task ->
                                           task.getOutputs()
                                                   .stream()
                                                   .filter(file -> fileFilterByType(file, STUDY_POINTS))
                                                   .map(this::getStudyPointResult)
                                                   .forEach(studyPoint -> studyPointsPerTask.put(task, studyPoint))
            );
        }
    }

    private boolean fileFilterByType(final ProcessFileDto file, final String type) {
        return file.getProcessFileStatus() == VALIDATED && type.equals(file.getFileType());
    }

    private static int getOutputFileVersion(final Set<TaskDto> tasksToPostProcess) {
        return tasksToPostProcess
            .stream()
            .mapToInt(task -> task.getRunHistory().size())
            .max()
            .orElse(1);
    }

    private List<IvaBranchData> getIvaResult(final ProcessFileDto processFileDto) {
        try (final InputStream inputStream = minioAdapter.getFileFromFullPath(processFileDto.getFilePath())) {
            final TypeReference<List<IvaBranchData>> ivaListTypeRef = new TypeReference<>() {
            };
            return new ObjectMapper().reader().forType(ivaListTypeRef).readValue(inputStream);
        } catch (final IOException e) {
            throw new CoreValidD2PostProcessingInvalidDataException("Error retrieving IVA Result", e);
        }
    }

    private List<StudyPoint> getStudyPointResult(final ProcessFileDto processFileDto) {
        try (final InputStream inputStream = minioAdapter.getFileFromFullPath(processFileDto.getFilePath())) {
            final TypeReference<List<StudyPoint>> studyPointListTypeRef = new TypeReference<>() {
            };
            return new ObjectMapper().reader().forType(studyPointListTypeRef).readValue(inputStream);
        } catch (final IOException e) {
            throw new CoreValidD2PostProcessingInvalidDataException("Error retrieving Study Point Result", e);
        }
    }

    private String getOutputFileName(final String pattern,
                                     final LocalDate localDate,
                                     final int outputFileVersion) {

        return String.format(pattern, localDate.format(YYYYMMDD_FORMATTER), outputFileVersion);
    }

    private byte[] marshallMessageAndSetValidationTypeComment(final FlowBasedConstraintUpdateDocument constraintUpdateDocument) {
        try {
            final StringWriter stringWriter = new StringWriter();
            final Marshaller jaxbMarshaller = JAXB_CONTEXT.createMarshaller();
            jaxbMarshaller.setProperty(JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.setProperty(JAXB_NO_NAMESPACE_SCHEMA_LOCATION, XSD_FILE_NAME);

            final QName elementName = JAXB_CONTEXT
                .createJAXBIntrospector()
                .getElementName(constraintUpdateDocument);

            final boolean hasAdjustmentValues = constraintUpdateDocument.getAdjustmentValues() != null;
            final JAXBElement<FlowBasedConstraintUpdateDocument> root = new JAXBElement<>(elementName,
                                                                                          FlowBasedConstraintUpdateDocument.class,
                                                                                          constraintUpdateDocument);
            jaxbMarshaller.marshal(root, stringWriter);
            final String fileXmlString = stringWriter.toString();
            final String commentedXML = fileXmlString.replace(DOMAIN_END_HEADER, DOMAIN_END_HEADER
                                                                                 + VALIDATION_TYPE_COMMENT
                                                                                 + NO_BRANCH_COMMENT
                                                                                 + (hasAdjustmentValues ? "" : NO_ADJUSTMENT_COMMENT));
            return commentedXML.getBytes(UTF_8);
        } catch (final Exception e) {
            throw new CoreValidD2PostProcessingInternalException("Exception occurred during constraint update document export.", e);
        }
    }

    private static JAXBContext initJaxbContext() {
        try {
            return JAXBContext.newInstance(FlowBasedConstraintUpdateDocument.class);
        } catch (JAXBException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
