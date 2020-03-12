package gov.cms.ab2d.optout;

import java.io.BufferedReader;

public interface OptOutImporter {

    void process(BufferedReader bufferedReader, String filename);

}
