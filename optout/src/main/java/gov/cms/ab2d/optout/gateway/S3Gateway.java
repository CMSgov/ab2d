package gov.cms.ab2d.optout.gateway;

import java.io.InputStreamReader;

public interface S3Gateway {

    InputStreamReader getOptOutFile();

}
