package gov.cms.ab2d.hpms.processing;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.service.SponsorService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Optional;

import static gov.cms.ab2d.common.util.Constants.SPONSOR_LOG;
import static net.logstash.logback.argument.StructuredArguments.keyValue;

@Slf4j
@Transactional
@Service("orgStructureReportProcessor")
public class OrgStructureReportProcessor implements ExcelReportProcessor {

    @Autowired
    private SponsorService sponsorService;

    @Override
    public void processReport(InputStream xlsInputStream, ExcelType excelType) throws IOException {
        try (Workbook workbook = excelType.getWorkbookType(xlsInputStream)) {
            Sheet datatypeSheet = workbook.getSheetAt(0);
            Iterator<Row> iterator = datatypeSheet.iterator();

            log.info("Beginning processing a total of {} rows", datatypeSheet.getPhysicalNumberOfRows());

            while (iterator.hasNext()) {

                Row currentRow = iterator.next();
                if (currentRow == null) {
                    continue;
                }
                Cell contractNumberCell = currentRow.getCell(4);
                if (contractNumberCell == null) {
                    continue;
                }
                // Contract number can occasionally be a numeric cell, so we need to account for
                // that
                if (contractNumberCell.getCellType() != CellType.STRING) {
                    continue;
                }
                String contractNumber = contractNumberCell.getStringCellValue();
                if (contractNumber == null) {
                    continue;
                }
                if (contractNumber.toUpperCase().startsWith("E") ||
                        contractNumber.toUpperCase().startsWith("S")) {
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

                    Optional<Sponsor> sponsorOptional =
                            sponsorService.findByHpmsIdAndParent(sponsorHpmsId.intValue(),
                                    parentSponsor);

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
        }
    }
}
