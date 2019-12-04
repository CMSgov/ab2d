package gov.cms.ab2d.optout;

import gov.cms.ab2d.common.model.Consent;

import java.util.Optional;

public interface ConsentConverterService {

    /**
     * Given a line from a file, and its corresponding line number,
     * converts the string into a Consent onject that can be persisted in the database.
     *
     * @param line
     * @param filename
     * @param lineNum
     * @return
     */
    Optional<Consent> convert(String line, String filename, int lineNum);
}
