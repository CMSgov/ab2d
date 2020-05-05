package gov.cms.ab2d.hpms.processing;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.service.SponsorService;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.eventlogger.events.ReloadEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static gov.cms.ab2d.common.util.Constants.SPONSOR_LOG;
import static gov.cms.ab2d.common.util.Constants.USERNAME;
import static gov.cms.ab2d.eventlogger.events.ReloadEvent.FileType.UPLOAD_ORG_STRUCTURE_REPORT;
import static net.logstash.logback.argument.StructuredArguments.keyValue;

@Slf4j
@Transactional
@Service("orgStructureReportProcessor")
public class OrgStructureReportProcessor implements ExcelReportProcessor {

    @Autowired
    private LogManager eventLogger;

    @Autowired
    private SponsorService sponsorService;

    @Override
    public void processReport(String fileName, InputStream xlsInputStream, ExcelType excelType) throws IOException {
        try (Workbook workbook = excelType.getWorkbookType(xlsInputStream)) {
            Sheet datatypeSheet = workbook.getSheetAt(0);
            log.info("Beginning processing a total of {} rows", datatypeSheet.getPhysicalNumberOfRows());
            processSheet(datatypeSheet);
            eventLogger.log(new ReloadEvent(MDC.get(USERNAME), UPLOAD_ORG_STRUCTURE_REPORT, fileName, datatypeSheet.getPhysicalNumberOfRows()));
        }
    }

    private void processSheet(Sheet datatypeSheet) {
        var iterator = datatypeSheet.iterator();
        while (iterator.hasNext()) {
            Row currentRow = iterator.next();
            processRow(currentRow);
        }
    }

    private void processRow(Row currentRow) {
        if (currentRow == null) {
            return;
        }
        Cell contractNumberCell = currentRow.getCell(4);
        if (contractNumberCell == null) {
            return;
        }
        // Contract number can occasionally be a numeric cell, so we need to account for
        // that
        if (contractNumberCell.getCellType() != CellType.STRING) {
            return;
        }
        String contractNumber = contractNumberCell.getStringCellValue();
        if (contractNumber == null) {
            return;
        }
        if (contractNumber.toUpperCase().startsWith("E") ||
                contractNumber.toUpperCase().startsWith("S")) {
            linkSponsorWithContract(currentRow, contractNumber);
        }
    }

    private void linkSponsorWithContract(Row currentRow, String contractNumber) {
        String sponsorParentName = currentRow.getCell(0).getStringCellValue();
        Double sponsorParentHpmsId = currentRow.getCell(1).getNumericCellValue();
        String sponsorName = currentRow.getCell(2).getStringCellValue();
        Double sponsorHpmsId = currentRow.getCell(3).getNumericCellValue();
        String contractName = currentRow.getCell(5).getStringCellValue();

        Optional<Sponsor> parentSponsorOptional = sponsorService
                .findByHpmsIdAndParent(sponsorParentHpmsId.intValue(), null);

        Sponsor parentSponsor;
        if (parentSponsorOptional.isPresent()) {
            parentSponsor = parentSponsorOptional.get();
        } else {
            parentSponsor = new Sponsor();
            parentSponsor.setHpmsId(sponsorParentHpmsId.intValue());
            parentSponsor.setLegalName(sponsorParentName);
            parentSponsor.setOrgName(sponsorParentName);
            sponsorService.saveSponsor(parentSponsor);
        }

        Optional<Sponsor> sponsorOptional = sponsorService.findByHpmsIdAndParent(sponsorHpmsId.intValue(), parentSponsor);

        Sponsor sponsor;
        if (!sponsorOptional.isPresent()) {
            sponsor = new Sponsor();
        } else {
            sponsor = sponsorOptional.get();
        }

        log.info("Starting processing for sponsor {}", keyValue(SPONSOR_LOG, sponsorHpmsId));

        sponsor.setHpmsId(sponsorHpmsId.intValue());
        sponsor.setLegalName(sponsorName);
        sponsor.setOrgName(sponsorName);
        sponsor.setParent(parentSponsor);

        saveContractWithSponsor(sponsor, contractNumber, contractName, sponsorHpmsId);
    }

    private void saveContractWithSponsor(Sponsor sponsor, String contractNumber, String contractName, Double sponsorHpmsId) {
        // Only add the contract if it doesn't already exist
        if (!sponsor.hasContract(contractNumber)) {
            Contract contract = new Contract();
            contract.setContractName(contractName);
            contract.setContractNumber(contractNumber);
            contract.setSponsor(sponsor);

            sponsor.getContracts().add(contract);
        }

        sponsorService.saveSponsor(sponsor);

        log.info("Sponsor saved {}", keyValue(SPONSOR_LOG, sponsorHpmsId));
    }
}
