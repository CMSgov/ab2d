package gov.cms.ab2d.optout;

import java.io.InputStreamReader;

public interface S3Gateway {

//    ResponseInputStream<GetObjectResponse> getS3Object();

    InputStreamReader getS3Object();

}
