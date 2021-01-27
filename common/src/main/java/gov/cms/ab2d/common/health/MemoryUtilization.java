package gov.cms.ab2d.common.health;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;

/**
 * Health check on memory - make sure you aren't running to low
 */
@Slf4j
public final class MemoryUtilization {

    private MemoryUtilization() { }

    /**
     * Make sure you can allocate memory of at least a certain number of MB. If you get an out
     * of memory error, you can't so return true - you're out of memory.
     *
     * This is done with a two dimensional array. It's an array of one MB arrays of bytes. I split it up
     * so that we didn't have to have one contiguous set of memory
     *
     * @param numMBToCreate - the number of megabytes to create
     * @return if the JVM runs out of memory while trying to allocate the array
     */
    @SuppressFBWarnings
    public static boolean outOfMemory(int numMBToCreate) {
        try {

            byte[][] arr = new byte[numMBToCreate][1024];
            return false;
        } catch (OutOfMemoryError ex) {
            log.error("Error allocating memory - " + numMBToCreate + "MB", ex);
            return true;
        }
    }
}
