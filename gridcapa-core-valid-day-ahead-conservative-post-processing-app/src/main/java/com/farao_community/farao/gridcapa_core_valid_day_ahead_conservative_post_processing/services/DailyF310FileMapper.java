/*
 *  Copyright (c) 2025, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.services;

import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskParameterDto;
import com.farao_community.farao.gridcapa_core_valid_commons.vertex.Vertex;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative.api.domain.CnecRamData;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative.api.domain.IvaBranchData;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.exception.CoreValidD2PostProcessingInternalException;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.exception.CoreValidD2PostProcessingInvalidDataException;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.fbconstraint.xsd.AdjustmentValueType;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.fbconstraint.xsd.AdjustmentValuesType;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.fbconstraint.xsd.CircumstanceType;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.fbconstraint.xsd.FlowBasedConstraintUpdateDocument;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.fbconstraint.xsd.HubType;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.fbconstraint.xsd.NetpositionsType;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.fbconstraint.xsd.NpType;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.fbconstraint.xsd.ReportingInformationType;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.fbconstraint.xsd.etso.AreaType;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.fbconstraint.xsd.etso.IdentificationType;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.fbconstraint.xsd.etso.MessageDateTimeType;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.fbconstraint.xsd.etso.MessageType;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.fbconstraint.xsd.etso.PartyType;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.fbconstraint.xsd.etso.ProcessType;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.fbconstraint.xsd.etso.ProcessTypeList;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.fbconstraint.xsd.etso.RoleType;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.fbconstraint.xsd.etso.TimeIntervalType;
import com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.fbconstraint.xsd.etso.VersionType;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.CoreValidD2PostProcessingConstants.DATE_TIME_FORMAT;
import static com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.CoreValidD2PostProcessingConstants.DOC_ID_PATTERN;
import static com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.CoreValidD2PostProcessingConstants.JUSTIFICATION_MESSAGE_ID;
import static com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.CoreValidD2PostProcessingConstants.STRING_TYPE;
import static com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.CoreValidD2PostProcessingConstants.UTC_ZONE_ID;
import static com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.CoreValidD2PostProcessingConstants.XFR_RTE_Q_STRING_VALUE;
import static com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.CoreValidD2PostProcessingConstants.XTSO_CS_W_STRING_VALUE;
import static com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.CoreValidD2PostProcessingConstants.YFR_RTE_C_STRING_VALUE;
import static com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.CoreValidD2PostProcessingConstants.YYYYMMDD_FORMATTER;
import static com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.fbconstraint.xsd.etso.CodingSchemeType.A_01;
import static com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.fbconstraint.xsd.etso.MessageTypeList.B_07;
import static com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.fbconstraint.xsd.etso.RoleTypeList.A_04;
import static com.farao_community.farao.gridcapa_core_valid_day_ahead_conservative_post_processing.fbconstraint.xsd.etso.RoleTypeList.A_36;
import static java.math.BigDecimal.ZERO;
import static javax.xml.datatype.DatatypeConstants.FIELD_UNDEFINED;

public final class DailyF310FileMapper {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);

    private DailyF310FileMapper() {
        throw new IllegalStateException("Utility class");
    }

    public static void generateHeader(final LocalDate localDate,
                                      final int outputFileVersion,
                                      final FlowBasedConstraintUpdateDocument constraintUpdateDocument,
                                      final ZoneId zoneId) {

        final IdentificationType id = new IdentificationType();
        id.setV(String.format(XFR_RTE_Q_STRING_VALUE + DOC_ID_PATTERN,
                              localDate.format(YYYYMMDD_FORMATTER),
                              outputFileVersion));
        constraintUpdateDocument.setDocumentIdentification(id);

        final VersionType version = new VersionType();
        version.setV(outputFileVersion);
        constraintUpdateDocument.setDocumentVersion(version);

        final MessageType docType = new MessageType();
        docType.setV(B_07);
        constraintUpdateDocument.setDocumentType(docType);

        final ProcessType processType = new ProcessType();
        processType.setV(ProcessTypeList.A_01);
        constraintUpdateDocument.setProcessType(processType);

        final PartyType sender = new PartyType();
        sender.setCodingScheme(A_01);
        sender.setV(XFR_RTE_Q_STRING_VALUE);
        constraintUpdateDocument.setSenderIdentification(sender);

        final RoleType senderRole = new RoleType();
        senderRole.setV(A_04);
        constraintUpdateDocument.setSenderRole(senderRole);

        final PartyType receiver = new PartyType();
        receiver.setCodingScheme(A_01);
        receiver.setV(XTSO_CS_W_STRING_VALUE);
        constraintUpdateDocument.setReceiverIdentification(receiver);

        final RoleType receiverRole = new RoleType();
        receiverRole.setV(A_36);
        constraintUpdateDocument.setReceiverRole(receiverRole);
        generateCreationTime(constraintUpdateDocument);

        final TimeIntervalType constraintTimeInterval = new TimeIntervalType();
        constraintTimeInterval.setV(getHeaderTimeInterval(localDate, zoneId));
        constraintUpdateDocument.setConstraintTimeInterval(constraintTimeInterval);

        final AreaType domain = new AreaType();
        domain.setCodingScheme(A_01);
        domain.setV(YFR_RTE_C_STRING_VALUE);
        constraintUpdateDocument.setDomain(domain);
    }

    public static void generateBody(final FlowBasedConstraintUpdateDocument fbCtUpdateDoc,
                                    final Map<TaskDto, List<IvaBranchData>> ivaResultsPerTask) {
        final AdjustmentValuesType adjustmentValues = new AdjustmentValuesType();
        ivaResultsPerTask.forEach((task, ivaBranches) -> {
            if (ivaBranches != null && !ivaBranches.isEmpty()) {
                final Comparator<IvaBranchData> byNecId = (iv1, iv2)
                    -> iv2.getCnec().necId().compareToIgnoreCase(iv1.getCnec().necId());

                ivaBranches.stream()
                    .filter(iv -> ZERO.compareTo(iv.getConservativeIva()) != 0)
                    .sorted(byNecId)
                    .forEach(ivaBranchData -> generateReturnedAdjustments(ivaBranchData,
                                                                          getOneHourTimeInterval(task.getTimestamp()),
                                                                          adjustmentValues,
                                                                          getJustificationMessage(task.getParameters())));
            }
        });

        if (!adjustmentValues.getAdjustmentValue().isEmpty()) {
            fbCtUpdateDoc.setAdjustmentValues(adjustmentValues);
        }
    }

    private static String getJustificationMessage(final List<TaskParameterDto> taskParameters) {
        return taskParameters.stream()
            .filter(param -> STRING_TYPE.equalsIgnoreCase(param.getParameterType())
                             && JUSTIFICATION_MESSAGE_ID.equalsIgnoreCase(param.getId()))
            .map(taskParameterDto ->
                     Optional.ofNullable(taskParameterDto.getValue())
                         .orElse(taskParameterDto.getDefaultValue())
            )
            .findFirst()
            .orElseThrow(() -> new CoreValidD2PostProcessingInvalidDataException("No justification message found"));

    }

    private static void generateReturnedAdjustments(final IvaBranchData ivaBranchData,
                                                    final String taskDateTimeInterval,
                                                    final AdjustmentValuesType adjustmentValues,
                                                    final String justificationMessage) {
        final CnecRamData branch = ivaBranchData.getCnec();
        final String name = branch.neName() + " / " + branch.contingencyName();
        final AdjustmentValueType adjustmentValue = generateAdjustmentValue(name,
                                                                            ivaBranchData,
                                                                            taskDateTimeInterval,
                                                                            justificationMessage);
        adjustmentValues.getAdjustmentValue().add(adjustmentValue);
    }

    private static AdjustmentValueType generateAdjustmentValue(final String name,
                                                               final IvaBranchData ivaBranchData,
                                                               final String taskDateTimeInterval,
                                                               final String justificationMessage) {
        final AdjustmentValueType adjustmentValue = new AdjustmentValueType();
        adjustmentValue.setId(ivaBranchData.getCnec().necId());
        adjustmentValue.setName(name);

        final TimeIntervalType branchTimeInterval = new TimeIntervalType();
        branchTimeInterval.setV(taskDateTimeInterval);
        adjustmentValue.setTimeInterval(branchTimeInterval);
        adjustmentValue.setIVA(ivaBranchData.getConservativeIva().floatValue());

        adjustmentValue.setJustification(justificationMessage
                                         + " vertex "
                                         + ivaBranchData
                                             .getWorstVertices()
                                             .getFirst()
                                             .vertex()
                                             .vertexId());

        final ReportingInformationType reportingInformation = new ReportingInformationType();
        final PartyType tso = new PartyType();
        tso.setCodingScheme(A_01);
        tso.setV(XFR_RTE_Q_STRING_VALUE);
        reportingInformation.setTso(tso);

        final CircumstanceType circumstance = new CircumstanceType();
        final NetpositionsType netPositions = new NetpositionsType();
        final Vertex worstVertex = ivaBranchData.getWorstVertices().getFirst().vertex();
        generateNetPositions(worstVertex, netPositions);
        circumstance.setNetpositions(netPositions);
        reportingInformation.setCircumstance(circumstance);
        reportingInformation.setFallback(false);
        adjustmentValue.setReportingInformation(reportingInformation);

        return adjustmentValue;
    }

    private static void generateCreationTime(final FlowBasedConstraintUpdateDocument constraintUpdateDocument) {
        final MessageDateTimeType creationDateTime = new MessageDateTimeType();
        final ZonedDateTime now = ZonedDateTime.now(UTC_ZONE_ID);
        final XMLGregorianCalendar date = getXmlGregorianCalendar(now);
        creationDateTime.setV(date);
        constraintUpdateDocument.setCreationDateTime(creationDateTime);
    }

    private static void generateNetPositions(final Vertex worstVertex,
                                             final NetpositionsType netPositions) {
        final TreeMap<String, Integer> sortedCoordinates = new TreeMap<>(worstVertex.coordinates());
        sortedCoordinates.forEach((hubCode, value) -> extractNetPosition(netPositions, hubCode, value));
    }

    private static void extractNetPosition(final NetpositionsType netPositions,
                                           final String hubCode,
                                           final Integer value) {
        final NpType np = new NpType();
        final HubType hub = new HubType();
        hub.setName(hubCode);
        np.setHub(hub);
        np.setValue(value);
        netPositions.getNp().add(np);
    }

    private static String getOneHourTimeInterval(final OffsetDateTime dateTime) {

        final OffsetDateTime plusHourDateTime = dateTime.plus(Duration.ofHours(1));
        final ZonedDateTime startDateTime = dateTime.toZonedDateTime();
        final ZonedDateTime endDateTime = plusHourDateTime.toZonedDateTime();
        return getTimeInterval(startDateTime, endDateTime);
    }

    private static String getHeaderTimeInterval(final LocalDate localDate,
                                                final ZoneId zoneId) {
        final LocalDateTime localDateTime = localDate.atTime(0, 0);
        final ZonedDateTime startTimestamp = getDateAtOffsetConvertedToUtc(localDateTime, zoneId);
        final ZonedDateTime endTimestamp = getDateAtOffsetConvertedToUtc(localDateTime.plusDays(1), zoneId);
        return getTimeInterval(startTimestamp, endTimestamp);
    }

    private static String getTimeInterval(final ZonedDateTime startDateTime,
                                          final ZonedDateTime endDateTime) {
        return startDateTime.format(FORMATTER) + "/" + endDateTime.format(FORMATTER);
    }

    private static ZonedDateTime getDateAtOffsetConvertedToUtc(final LocalDateTime localDateTime,
                                                               final ZoneId zoneId) {
        final ZoneOffset zoneOffset = zoneId.getRules().getOffset(localDateTime);
        return localDateTime.atOffset(zoneOffset).atZoneSameInstant(UTC_ZONE_ID);
    }

    private static XMLGregorianCalendar getXmlGregorianCalendar(final ZonedDateTime zonedDateTime) {
        try {
            final GregorianCalendar cal = GregorianCalendar.from(zonedDateTime);
            final XMLGregorianCalendar date = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
            date.setMillisecond(FIELD_UNDEFINED);
            date.setTimezone(0);
            return date;
        } catch (final DatatypeConfigurationException e) {
            throw new CoreValidD2PostProcessingInternalException("Invalid date format", e);
        }
    }
}
