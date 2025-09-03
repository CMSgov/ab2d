package gov.cms.ab2d.aggregator;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static gov.cms.ab2d.aggregator.FileOutputType.DATA;
import static gov.cms.ab2d.aggregator.FileOutputType.ERROR;
import static gov.cms.ab2d.aggregator.FileUtils.createADir;
import static gov.cms.ab2d.aggregator.FileUtils.deleteAllInDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileUtilsTest {
    private static final String JAVA_TMPDIR = "java.io.tmpdir";
    private static final String FILE_1 = "file1";
    private static final String FILE_2 = "file2";
    private static final String TST_DIR = "tstdir";
    private static final String SUBDIR = "subdir";
    private static final String TXT = ".txt";
    private static final String BOGUS_FILE = "bogusFile.txt";

    @Test
    void combineFiles() throws IOException {
        File fulltmpdir = createADir(System.getProperty(JAVA_TMPDIR) + "/abc");
        try {
            assertEquals(0, FileUtils.getSizeOfFiles(fulltmpdir.getAbsolutePath(), ERROR));
            assertEquals(0, FileUtils.getSizeOfFiles(fulltmpdir.getAbsolutePath(), DATA));

            String data1 = "aaaaaaaa\n";
            String data2 = "bbbbbbbbbbbbbbb\n";
            String data3 = "ccccc\n";

            Path p1 = createFile(fulltmpdir, FILE_1 + DATA.getSuffix(), data1);
            Path p2 = createFile(fulltmpdir, FILE_2 + ERROR.getSuffix(), data2);
            Path p3 = createFile(fulltmpdir, "file3" + ERROR.getSuffix(), data3);

            List<File> fileA = new ArrayList<>();
            List<File> fileB = List.of(p1.toFile());
            List<File> fileC = List.of(p1.toFile(), p2.toFile());
            List<File> fileD = List.of(p1.toFile(), p2.toFile(), p3.toFile());

            FileUtils.combineFiles(fileA, fulltmpdir.getAbsolutePath() + "/tst1");
            assertEquals(0, Files.size(Path.of(fulltmpdir.getAbsolutePath(), "tst1")));

            FileUtils.combineFiles(fileB, fulltmpdir.getAbsolutePath() + "/tst2");
            assertEquals(data1.length(), Files.size(Path.of(fulltmpdir.getAbsolutePath(), "tst2")));

            FileUtils.combineFiles(fileC, fulltmpdir.getAbsolutePath() + "/tst3");
            assertEquals(data1.length() + data2.length(), Files.size(Path.of(fulltmpdir.getAbsolutePath(), "tst3")));

            FileUtils.combineFiles(fileD, fulltmpdir.getAbsolutePath() + "/tst4");
            assertEquals(data1.length() + data2.length() + data3.length(), Files.size(Path.of(fulltmpdir.getAbsolutePath(), "tst4")));

            byte[] bytes = Files.readAllBytes(Path.of(fulltmpdir.getAbsolutePath() + "/tst4"));
            assertEquals(new String(bytes), data1 + data2 + data3);

            assertTrue(FileUtils.cleanUpFiles(fileD));
            assertTrue(FileUtils.cleanUpFiles(null));

            assertFalse(p1.toFile().exists());
            assertFalse(p2.toFile().exists());
            assertFalse(p3.toFile().exists());

        } finally {
            assertTrue(deleteAllInDir(fulltmpdir));
        }
    }

    @Test
    void getSizeOfFiles() throws IOException {
        File fulltmpdir = createADir(System.getProperty(JAVA_TMPDIR) + "/abc");
        createADir(fulltmpdir.getAbsolutePath());
        FileUtils.deleteAllInDir(fulltmpdir);
        try {
            assertEquals(0, FileUtils.getSizeOfFiles(fulltmpdir.getAbsolutePath(), ERROR));
            assertEquals(0, FileUtils.getSizeOfFiles(fulltmpdir.getAbsolutePath(), DATA));

            String data1 = "aaaaaaaa";
            String data2 = "bbbbbbbbbbbbbbb";
            String data3 = "ccccc";
            String data4 = "ddddddddddddddddddddd";

            createFile(fulltmpdir, FILE_1 + DATA.getSuffix(), data1);

            assertEquals(data1.length(), FileUtils.getSizeOfFiles(fulltmpdir.getAbsolutePath(), DATA));
            assertEquals(0, FileUtils.getSizeOfFiles(fulltmpdir.getAbsolutePath(), ERROR));

            createFile(fulltmpdir, FILE_2 + ERROR.getSuffix(), data2);

            assertEquals(data1.length(), FileUtils.getSizeOfFiles(fulltmpdir.getAbsolutePath(), DATA));
            assertEquals(data2.length(), FileUtils.getSizeOfFiles(fulltmpdir.getAbsolutePath(), ERROR));

            createFile(fulltmpdir, "file3" + ERROR.getSuffix(), data3);

            createFile(fulltmpdir, "file4" + DATA.getSuffix(), data4);

            assertEquals(data1.length() + data4.length(), FileUtils.getSizeOfFiles(fulltmpdir.getAbsolutePath(), DATA));
            assertEquals(data2.length() + data3.length(), FileUtils.getSizeOfFiles(fulltmpdir.getAbsolutePath(), ERROR));
        } finally {
            assertTrue(deleteAllInDir(fulltmpdir));
        }
    }

    @Test
    void testExceptions() {
        assertEquals(0, FileUtils.getSizeOfFiles(null));
        assertEquals(0, FileUtils.getSizeOfFiles(new ArrayList<>()));
        assertEquals(0, FileUtils.getSizeOfFiles(Collections.singletonList(new File(File.separator + BOGUS_FILE))));
    }

    @Test
    void listFiles() throws IOException {
        File fulltmpdir = createADir(System.getProperty(JAVA_TMPDIR) + File.separator + TST_DIR);

        try {
            List<File> fileListEmpty = FileUtils.listFiles(fulltmpdir.getAbsolutePath(), DATA);
            assertEquals(0, fileListEmpty.size());

            createFile(fulltmpdir, FILE_1 + DATA.getSuffix(),
                    "file 1 data");

            createFile(fulltmpdir, FILE_2 + ERROR.getSuffix(),
                    "file 2 data");

            List<File> fileList1 = FileUtils.listFiles(fulltmpdir.getAbsolutePath(), DATA);
            assertEquals(1, fileList1.size());
            assertTrue(fileList1.get(0).getName().contains(FILE_1));

            List<File> fileList2 = FileUtils.listFiles(fulltmpdir.getAbsolutePath(), ERROR);
            assertEquals(1, fileList2.size());
            assertTrue(fileList2.get(0).getName().contains(FILE_2));
        } finally {
            assertTrue(deleteAllInDir(fulltmpdir));
        }
    }

    @Test
    void getSizeOfFileOrDirectory() throws IOException {
        File fulltmpdir = createADir(System.getProperty(JAVA_TMPDIR) + File.separator + TST_DIR);

        try {
            String data1 = "aaaaaaaa";
            String data2 = "bbbbbbbbbbbbbbb";

            Path p1 = createFile(fulltmpdir, FILE_1 + DATA.getSuffix(), data1);
            Path p2 = createFile(fulltmpdir, FILE_2 + ERROR.getSuffix(), data2);

            assertEquals(data1.length(), FileUtils.getSizeOfFileOrDirectory(p1.toFile().getAbsolutePath()));
            assertEquals(data2.length(), FileUtils.getSizeOfFileOrDirectory(p2.toFile().getAbsolutePath()));

            assertEquals(0, FileUtils.getSizeOfFileOrDirectory(BOGUS_FILE));
        } finally {
            assertTrue(deleteAllInDir(fulltmpdir));
        }
    }

    @Test
    void testDeleteAllInDir() throws IOException {
        File fulltmpdir = createADir(System.getProperty(JAVA_TMPDIR) + File.separator + TST_DIR);
        assertTrue(fulltmpdir.exists());
        assertNotNull(createFile(fulltmpdir, FILE_1 + TXT, "ABCD"));
        assertTrue(deleteAllInDir(fulltmpdir));
        assertFalse(fulltmpdir.exists());

        fulltmpdir = createADir(System.getProperty(JAVA_TMPDIR) + File.separator + TST_DIR);
        assertNotNull(createFile(fulltmpdir, FILE_1 + TXT, "ABCD"));
        assertNotNull(createFile(fulltmpdir, FILE_2 + TXT, "EFGH"));
        File fulltmpdirSub = createADir(System.getProperty(JAVA_TMPDIR) + File.separator + TST_DIR + File.separator + SUBDIR);
        assertNotNull(createFile(fulltmpdirSub, "file3.txt", "IJKL"));
        File[] files = fulltmpdir.listFiles();
        assertNotNull(files);
        assertEquals(3, files.length);
        assertTrue(files[0].getName().equals(FILE_1 + TXT) || files[0].getName().equals(FILE_2 + TXT) || files[0].getName().equals(SUBDIR));
        assertTrue(files[1].getName().equals(FILE_1 + TXT) || files[1].getName().equals(FILE_2 + TXT) || files[1].getName().equals(SUBDIR));
        assertTrue(files[2].getName().equals(FILE_1 + TXT) || files[2].getName().equals(FILE_2 + TXT) || files[2].getName().equals(SUBDIR));
        assertTrue(deleteAllInDir(fulltmpdir));
        assertFalse(fulltmpdirSub.exists());
        assertFalse(fulltmpdir.exists());
    }

    @Test
    void testDeleteAllInEmptyDirs() throws IOException {
        File dir = createADir(System.getProperty(JAVA_TMPDIR) + File.separator + TST_DIR);
        assertTrue(dir.exists());
        assertTrue(deleteAllInDir(dir));
        assertFalse(dir.exists());

        assertTrue(deleteAllInDir(new File("does-not-exist")));
    }

    static Path createFile(File dir, String fileName, String data) throws IOException {
        Files.createDirectories(Path.of(dir.getAbsolutePath()));
        Path p = Path.of(dir.getAbsolutePath(), fileName);
        assertTrue(p.toFile().createNewFile());
        Files.writeString(p, data);
        return p;
    }
}
