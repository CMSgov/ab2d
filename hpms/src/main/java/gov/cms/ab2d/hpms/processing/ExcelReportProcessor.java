package gov.cms.ab2d.hpms.processing;

import java.io.IOException;
import java.io.InputStream;

public interface ExcelReportProcessor {

    void processReport(InputStream xlsInputStream, ExcelType excelType) throws IOException;
}
