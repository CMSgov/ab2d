package gov.cms.ab2d.aggregator;

import org.junit.jupiter.api.Test;

import static gov.cms.ab2d.aggregator.FileOutputType.DATA;
import static gov.cms.ab2d.aggregator.FileOutputType.DATA_COMPRESSED;
import static gov.cms.ab2d.aggregator.FileOutputType.ERROR;
import static gov.cms.ab2d.aggregator.FileOutputType.ERROR_COMPRESSED;
import static gov.cms.ab2d.aggregator.FileOutputType.UNKNOWN;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

class FileOutputTypeTest {

    @Test
    void isErrorFile() {
        String file1 = "abc." + ERROR.getSuffix();
        String file2 = "abc." + DATA.getSuffix();
        String file3 = "abc." + ERROR_COMPRESSED.getSuffix();
        String file4 = "abc." + DATA_COMPRESSED.getSuffix();
        assertEquals(ERROR, FileOutputType.getFileType(file1));
        assertEquals(DATA, FileOutputType.getFileType(file2));
        assertEquals(ERROR_COMPRESSED, FileOutputType.getFileType(file3));
        assertEquals(DATA_COMPRESSED, FileOutputType.getFileType(file4));
        assertEquals(UNKNOWN, FileOutputType.getFileType("bogus.txt"));
        assertEquals(UNKNOWN, FileOutputType.getFileType((String) null));
    }

    @Test
    void testActualFile() {
        File file1 = new File("abc." + ERROR.getSuffix());
        File file2 = new File("abc." + DATA.getSuffix());
        File file3 = new File("abc." + ERROR_COMPRESSED.getSuffix());
        File file4 = new File("abc." + DATA_COMPRESSED.getSuffix());
        assertEquals(ERROR, FileOutputType.getFileType(file1));
        assertEquals(DATA, FileOutputType.getFileType(file2));
        assertEquals(ERROR_COMPRESSED, FileOutputType.getFileType(file3));
        assertEquals(DATA_COMPRESSED, FileOutputType.getFileType(file4));
    }
}
