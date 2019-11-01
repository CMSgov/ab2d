package gov.cms.ab2d.hpms.processing;

import gov.cms.ab2d.common.model.Attestation;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Sponsor;
//import gov.cms.ab2d.common.service.ContractService;
import gov.cms.ab2d.common.service.SponsorService;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.OffsetDateTime;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

@Service("hpmsExcelReportProcessor")
public class HPMSExcelReportProcessor implements ExcelReportProcessor {

    @Autowired
    private SponsorService sponsorService;

    //@Autowired
    //private ContractService contractService;

    @Override
    public void processReport(InputStream xlsFile) throws IOException {
        try (Workbook workbook = new HSSFWorkbook(xlsFile)) {
            Sheet datatypeSheet = workbook.getSheetAt(0);
            Iterator<Row> iterator = datatypeSheet.iterator();

            while (iterator.hasNext()) {

                Row currentRow = iterator.next();
                if (currentRow == null) {
                    continue;
                }
                Cell contractNumberCell = currentRow.getCell(4);
                if (contractNumberCell == null) {
                    continue;
                }
                // Contract number can occasionally be a numeric cell, so we need to account for that
                if (contractNumberCell.getCellType() != CellType.STRING) {
                    continue;
                }
                String contractNumber = contractNumberCell.getStringCellValue();
                if (contractNumber == null) {
                    continue;
                }
                if (contractNumber.startsWith("E") || contractNumber.startsWith("S")) {
                    String sponsorParentName = currentRow.getCell(0).getStringCellValue();
                    Double sponsorParentHpmsId = currentRow.getCell(1).getNumericCellValue();
                    String sponsorName = currentRow.getCell(2).getStringCellValue();
                    Double sponsorHpmsId = currentRow.getCell(3).getNumericCellValue();
                    String contractName = currentRow.getCell(5).getStringCellValue();

                    Optional<Sponsor> sponsorOptional = sponsorService.getSponsorByHpmsId(sponsorHpmsId.intValue());
                    Optional<Sponsor> parentSponsorOptional = sponsorService.getSponsorByHpmsId(sponsorParentHpmsId.intValue());

                    Sponsor sponsor = sponsorOptional.orElse(new Sponsor());

                    sponsor.setHpmsId(sponsorHpmsId.intValue());
                    sponsor.setLegalName(sponsorName);
                    sponsor.setContractName(contractName);

                    Attestation attestation = new Attestation();
                    attestation.setSponsor(sponsor);
                    attestation.setAttestedOn(OffsetDateTime.now());

                    Contract contract = new Contract();
                    contract.setAttestations(Set.of(attestation));

                    attestation.setContract(contract);

                    sponsor.setAttestations(Set.of(attestation));

                    if (parentSponsorOptional.isPresent()) {
                        sponsor.setParent(parentSponsorOptional.get());
                    } else {
                        Sponsor parent = new Sponsor();
                        parent.setHpmsId(sponsorParentHpmsId.intValue());
                        parent.setLegalName(sponsorParentName);
                        sponsor.setParent(parent);
                        sponsorService.saveSponsor(parent);
                    }
                    /*Sponsor parent = parentSponsorOptional.orElse(new Sponsor());
                    parent.setHpmsID(sponsorParentHpmsId.intValue());
                    parent.setLegalName(sponsorParentName);
                    sponsor.setParent(parent);
                    sponsorService.saveSponsor(parent);*/

                    sponsorService.saveSponsor(sponsor);
                }
            }
        }
    }
}
