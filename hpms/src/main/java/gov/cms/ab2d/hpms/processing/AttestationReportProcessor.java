package gov.cms.ab2d.hpms.processing;

import gov.cms.ab2d.common.model.Attestation;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.service.AttestationService;
import gov.cms.ab2d.common.service.ContractService;
import gov.cms.ab2d.common.util.AttestationStatus;
import gov.cms.ab2d.common.util.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;


@Service("attestationReportProcessor")
@Transactional
@Slf4j
public class AttestationReportProcessor implements ExcelReportProcessor {

    @Autowired
    private ContractService contractService;

    @Autowired
    private AttestationService attestationService;

    private static final String ATTESTATION_OFFSET_DATE_TIME_PATTERN = "M/d/y h:m a Z";

    @Override
    public void processReport(InputStream xlsInputStream, ExcelType excelType) throws IOException {
        try (Workbook workbook = excelType.getWorkbookType(xlsInputStream)) {
            Sheet datatypeSheet = workbook.getSheetAt(0);
            Iterator<Row> iterator = datatypeSheet.iterator();

            Set<String> contractIdsSeen = new HashSet<>();

            while (iterator.hasNext()) {

                Row currentRow = iterator.next();
                if (currentRow == null) {
                    continue;
                }

                String contractNumber = currentRow.getCell(0).getStringCellValue();
                // This report is ordered by contract number and date DESC, so just move on to the next row if we've already seen a contract
                if (contractIdsSeen.contains(contractNumber)) {
                    continue;
                }

                contractIdsSeen.add(contractNumber);

                Optional<Contract> contractOptional = contractService.getContractByContractId(contractNumber);

                if (contractOptional.isPresent()) {
                    Contract contract = contractOptional.get();
                    String attestationStatus = currentRow.getCell(2).getStringCellValue();
                    Attestation attestation = attestationService.getMostRecentAttestationFromContract(contract);
                    if (attestationStatus.toUpperCase().equals(AttestationStatus.ATTESTED.getValue().toUpperCase())) {
                        String attestetedDateTimeCell = currentRow.getCell(6).getStringCellValue();
                        // Set to Eastern time since that's where HPMS is
                        String offset = DateUtil.getESTOffset();
                        OffsetDateTime offsetDateTime = OffsetDateTime.parse(attestetedDateTimeCell + " " + offset,
                                DateTimeFormatter.ofPattern(ATTESTATION_OFFSET_DATE_TIME_PATTERN));
                        attestation.setAttestedOn(offsetDateTime);
                    } else {
                        attestation.setAttestedOn(null);
                    }

                    attestationService.saveAttestation(attestation);
                } else {
                    log.warn("Contract ID {} was not found in the database during contract report processing", contractNumber);
                }
            }
        }
    }
}
