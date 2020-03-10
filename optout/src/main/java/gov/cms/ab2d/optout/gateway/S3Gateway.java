package gov.cms.ab2d.optout.gateway;

import java.io.InputStreamReader;
import java.util.List;

public interface S3Gateway {

    /**
     * @return a list of opt-out file-names from the S3 bucket
     */
    List<String> listOptOutFiles();

    /**
     * Given a filename
     * @return the contents of the file
     */
    InputStreamReader getOptOutFile(String fileName);

}
