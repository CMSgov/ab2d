package gov.cms.ab2d.common.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

@UtilityClass
@Slf4j
public class GzipCompressUtils {

    public static void compress(final InputStream inputStream, final OutputStream out) throws IOException {
        try (BufferedOutputStream outputBuffer = new BufferedOutputStream(out);
             GzipCompressorOutputStream compressor = new GzipCompressorOutputStream(outputBuffer)) {
            org.apache.commons.io.IOUtils.copy(inputStream, compressor);
        }
    }

    public static void compress(final Path uncompressedFile, final OutputStream out) throws IOException {
        try (InputStream inputStream = Files.newInputStream(uncompressedFile)) {
            compress(inputStream, out);
        }
    }

    public static void compress(final Path uncompressedFile, final Path destination) throws IOException {
        try (OutputStream out = Files.newOutputStream(destination)) {
            compress(uncompressedFile, out);
        }
    }

    public static void decompress(final InputStream inputStream, final OutputStream out) throws IOException {
        try (BufferedInputStream inputBuffer = new BufferedInputStream(inputStream);
             GzipCompressorInputStream decompressor = new GzipCompressorInputStream(inputBuffer)) {
            org.apache.commons.io.IOUtils.copy(decompressor, out);
        }
    }

    public static void decompress(final Path compressedFile, final OutputStream out) throws IOException {
        try (InputStream inputStream = Files.newInputStream(compressedFile)) {
            decompress(inputStream, out);
        }
    }

    public static void decompress(final Path compressedFile, Path destination) throws IOException {
        try (OutputStream out = Files.newOutputStream(destination)) {
            decompress(compressedFile, out);
        }
    }

    /**
     * Compress job output files (both 'DATA' and 'ERROR' types)
     * @param jobId job id
     * @param baseDir root directory containing directory for corresponding job id
     * @param fileFilter function to determine whether a file should be compressed
     * @return false if an error occurred while compressing one or more files (unlikely), true otherwise
     */
    /*
    public static boolean compressJobOutputFiles(
            String jobId,
            String baseDir,
            FileFilter fileFilter) {
        val jobDirectory = new File(baseDir + File.separator + jobId);
        if (!jobDirectory.exists() || !jobDirectory.isDirectory()) {
            return false;
        }

        val files = jobDirectory.listFiles(fileFilter);
        boolean success = true;
        for (File file : files) {
            success = success && compressFile(file, true);
        }
        return success;
    }
    */

    /**
     * Compress a file (outputting to the same directory) and optionally delete file after compressing
     * @param file file to compress
     * @param deleteFile if true, delete file after compressing
     * @return the output file if file was compressed successfully, null otherwise
     */
    public static File compressFile(File file, boolean deleteFile) {
        if (file == null || !file.exists() || !file.isFile()) {
            return null;
        }
        // append ".gz" to the input filename
        val compressedOutputFile = new File(file.getParent(), file.getName() + ".gz");
        try {
            GzipCompressUtils.compress(file.toPath(), compressedOutputFile.toPath());
        } catch (IOException e) {
            log.error("Unable to compress file: {}", file.getAbsoluteFile());
            return null;
        }
        if (deleteFile) {
            try {
                Files.delete(file.toPath());
            } catch (IOException e) {
                log.error("Unable to delete file: {}", file.getAbsolutePath());
            }
        }
        return compressedOutputFile;
    }
}