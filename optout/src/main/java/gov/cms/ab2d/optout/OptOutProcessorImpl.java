package gov.cms.ab2d.optout;


import gov.cms.ab2d.common.model.Consent;
import gov.cms.ab2d.common.repository.ConsentRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.Optional;

@Slf4j
@Component
public class OptOutProcessorImpl implements OptOutProcessor {

    @Autowired
    private ConsentRepository consentRepository;

    @Autowired
    private ConsentConverterService  consentConverterService;

    @Autowired
    private S3Gateway s3Gateway;

    @Override
    @Transactional
    public void process() {

//        //set region
//        final Region region = Region.of(s3Region);
//
//
//        // build S3 client
//        final S3Client s3Client =  S3Client.builder().region(region).build();
//
//
//        // build GetObjectRequest
//        final GetObjectRequest getObjectRequest = GetObjectRequest.builder()
//                .bucket(s3Bucket)
//                .key(s3Filename)
//                .build();
//
//
//
//        long linesReadCount = 0;
//        ResponseInputStream<GetObjectResponse> responseInputStream = null;
//        try {
//            responseInputStream = s3Client.getObject(getObjectRequest);
//        } catch (SdkServiceException e) {
//            log.error("Server error upon calling AWS  : ", e);
//            throw e;
//        } catch (SdkClientException e) {
//            log.error("Client exception on attempting to call AWS : ", e);
//            throw e;
//        }

//        ResponseInputStream<GetObjectResponse> responseInputStream = s3Gateway.getS3Object();
//        InputStreamReader inputStreamReader = new InputStreamReader(responseInputStream);

        final InputStreamReader inputStreamReader = s3Gateway.getS3Object();

        try (var bufferedReader = new BufferedReader(inputStreamReader)) {
            processReader(bufferedReader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

    }

    private void processReader(BufferedReader bufferedReader) {
        var lineIter = IOUtils.lineIterator(bufferedReader);
        int lineNum = 0;

        while (lineIter.hasNext()) {
            lineNum++;

            try {
                Optional<Consent> optConsent = consentConverterService.convert(lineIter.nextLine(), "s3Filename", lineNum);
                if (optConsent.isPresent()) {
                    consentRepository.save(optConsent.get());
                }
            } catch (Exception e) {
                log.error("Invalid opt out record ", e);

            }
        }

    }


}
