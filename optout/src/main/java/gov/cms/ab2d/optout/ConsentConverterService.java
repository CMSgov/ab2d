package gov.cms.ab2d.optout;

import gov.cms.ab2d.common.model.Consent;

import java.util.Optional;

public interface ConsentConverterService {

    /**
     * Given a line from a file, and its corresponding line number,
     * converts the string into a Consent object that can be persisted in the database.
     *
     * @param line
     *
     * @return (optional) Consent
     */
    Optional<Consent> convert(String line);
}
