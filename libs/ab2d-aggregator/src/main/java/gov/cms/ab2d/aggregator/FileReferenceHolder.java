package gov.cms.ab2d.aggregator;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.File;

/**
 * Holds the file and it's size. This enables us to manipulate ordering of files by only going to the file
 * system once. This is used for files that are finished streaming and in the finished directory and their size
 * won't change.
 */
@Getter
@AllArgsConstructor
public class FileReferenceHolder {
    private final File file;
    private final long size;
}
