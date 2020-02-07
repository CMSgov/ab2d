package gov.cms.ab2d.common.health;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
/**
 * This class has helps verify that you can write to the file system correctly
 */
public class FileSystemCheck {
    /**
     * Returns true if you can write to the file system in a particular directory or false if you can't.
     * This is done by creating a random file name, putting the current nano time in the file and verifying
     * that you can read the nanotime from the file and then clean up by deleting the file. It does not
     * create the directory unless createDir is true.
     *
     * @param dir - the directory to test
     * @param createDir - true if you want to create the directory before you write to it
     * @return true if you can write to it, false otherwise
     */
    public static boolean canWriteFile(String dir, boolean createDir) {
        if (dir == null || dir.isEmpty()) {
            return false;
        }
        Path dirPath = Paths.get(dir);
        if (Files.notExists(dirPath)) {
            if (createDir) {
                File directory = new File(dir);
                if (!directory.exists()) {
                    directory.mkdirs();
                }
            } else {
                return false;
            }
        }
        long currentTime = System.nanoTime();
        try {
            String file = dir + "/" + getRandomFileName(20, "txt");
            Path destination = Paths.get(file);
            String correctContent = "" + currentTime;
            Files.write(destination, correctContent.getBytes());
            List<String> lines = Files.readAllLines(destination);
            if (lines.size() != 1 || !lines.get(0).equalsIgnoreCase(correctContent)) {
                return false;
            }
            Files.delete(destination);
            return true;
        } catch (Exception ex) {
            log.error("Unable to write test file to dir " + dir, ex);
            return false;
        }
    }

    /**
     * Generate a random file of a certain length and extension type. The file name only contains characters A-Z & a-z.
     * and is appended by the "." + the extension.
     *
     * @param length - The length of the file to generate
     * @param ext - the extension, such as "txt"
     * @return - the generated file
     */
    public static String getRandomFileName(int length, String ext) {
        String alphas = "ABCDEFGHIJKLMNOPQRSTUVWXYabcdefghijklmnopqrstuvwxyz";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = (int) (alphas.length() * Math.random());
            sb.append(alphas.charAt(index));
        }
        return sb.toString() + "." + ext;
    }
}
