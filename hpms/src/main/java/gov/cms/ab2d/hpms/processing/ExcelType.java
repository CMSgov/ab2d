package gov.cms.ab2d.hpms.processing;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;

public enum ExcelType {

    XLS,
    XLSX;

    public Workbook getWorkbookType(InputStream inputStream) throws IOException {
        if (this.equals(XLS)) {
            return new HSSFWorkbook(inputStream);
        } else {
            return new XSSFWorkbook(inputStream);
        }
    }

    public static ExcelType fromFileType(String file) {
        if (file.toLowerCase().endsWith(".xls")) {
            return ExcelType.XLS;
        } else {
            return ExcelType.XLSX;
        }
    }
}
