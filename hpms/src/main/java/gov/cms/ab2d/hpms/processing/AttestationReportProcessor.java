package gov.cms.ab2d.hpms.processing;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.service.ContractService;
import gov.cms.ab2d.common.util.AttestationStatus;
import gov.cms.ab2d.common.util.DateUtil;
import gov.cms.ab2d.eventlogger.EventLogger;
import gov.cms.ab2d.eventlogger.events.ReloadEvent;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import static gov.cms.ab2d.common.util.Constants.CONTRACT_LOG;
import static gov.cms.ab2d.common.util.Constants.USERNAME;
import static gov.cms.ab2d.eventlogger.events.ReloadEvent.FileType.ATTESTATION_REPORT;
import static net.logstash.logback.argument.StructuredArguments.keyValue;


@Service("attestationReportProcessor")
@Transactional
@Slf4j
public class AttestationReportProcessor implements ExcelReportProcessor {

    @Autowired
    private ContractService contractService;

    @Autowired
    private EventLogger eventLogger;

    private static final String ATTESTATION_OFFSET_DATE_TIME_PATTERN = "M/d/y h:m a Z";

    @Value
    private class AttestationReportData {
        private Contract contract;
        private String attestationStatus;
        private OffsetDateTime attestedDateTime;
    }

    @Override
    public void processReport(String fileName, InputStream xlsInputStream, ExcelType excelType) throws IOException {
        try (Workbook workbook = excelType.getWorkbookType(xlsInputStream)) {
            Sheet datatypeSheet = workbook.getSheetAt(0);
            Iterator<Row> iterator = datatypeSheet.iterator();

            Map<String, AttestationReportData> contractsSeenToLatestAttestationData = new HashMap<>();

            log.info("Beginning processing a total of {} rows", datatypeSheet.getPhysicalNumberOfRows());

            // In this loop just gather the most recent attestation data
            while (iterator.hasNext()) {
                Row currentRow = iterator.next();

                if (currentRow == null) {
                    continue;
                }

                String contractNumber = currentRow.getCell(0).getStringCellValue();

                // Header, move on to next
                if (contractNumber.equals("Contract Number")) {
                    continue;
                }

                processRow(currentRow, contractsSeenToLatestAttestationData, contractNumber);
            }

            updateContract(contractsSeenToLatestAttestationData);
            eventLogger.log(new ReloadEvent(MDC.get(USERNAME), ATTESTATION_REPORT, fileName, datatypeSheet.getPhysicalNumberOfRows()));
        }
    }

    private void processRow(Row currentRow, Map<String, AttestationReportData> contractsSeenToLatestAttestationData,
                            String contractNumber) {
        Optional<Contract> contractOptional = contractService.getContractByContractNumber(contractNumber);
        if (contractOptional.isPresent()) {
            Contract contract = contractOptional.get();

            String attestetedDateTimeCell = currentRow.getCell(6).getStringCellValue();
            // Set to Eastern time since that's where HPMS is
            String offset = DateUtil.getESTOffset();
            OffsetDateTime offsetDateTime = OffsetDateTime.parse(attestetedDateTimeCell + " " + offset,
                    DateTimeFormatter.ofPattern(ATTESTATION_OFFSET_DATE_TIME_PATTERN));
            String attestationStatus = currentRow.getCell(2).getStringCellValue();

            if (!contractsSeenToLatestAttestationData.containsKey(contractNumber)) {
                contractsSeenToLatestAttestationData.put(contractNumber,
                        new AttestationReportData(contract, attestationStatus, offsetDateTime));
            } else {
                AttestationReportData latestData = contractsSeenToLatestAttestationData.get(contractNumber);
                // Overwrite if we have a later date, we only care about the latest
                if (offsetDateTime.isAfter(latestData.getAttestedDateTime())) {
                    contractsSeenToLatestAttestationData.put(contractNumber,
                            new AttestationReportData(contract, attestationStatus, offsetDateTime));
                }
            }
        } else {
            log.error("Contract ID {} was not found in the database during contract report " + "processing",
                    contractNumber);
            throw new ReportProcessingException("Contract ID " + contractNumber +
                    " was not found in the database during contract report processing");
        }
    }

    private void updateContract(Map<String, AttestationReportData> contractsSeenToLatestAttestationData) {
        for (Map.Entry<String, AttestationReportData> entry :
                contractsSeenToLatestAttestationData
                        .entrySet()) {
            AttestationReportData attestationReportData = entry.getValue();
            var contract = attestationReportData.getContract();

            String attestationStatus = attestationReportData.getAttestationStatus();
            OffsetDateTime offsetDateTime = attestationStatus.toUpperCase()
                    .equals(AttestationStatus.ATTESTED.getValue().toUpperCase()) ?
                    attestationReportData.getAttestedDateTime() : null;
            contract.setAttestedOn(offsetDateTime);
            contractService.updateContract(contract);

            log.info("Updated contract {}", keyValue(CONTRACT_LOG, contract.getContractName()));
        }
    }
}
