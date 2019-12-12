package gov.cms.ab2d.optout;

import gov.cms.ab2d.common.model.OptOut;

import java.util.Optional;

public interface OptOutConverterService {

    /**
     * Given a line from a file,
     * converts the string into a OptOut object that can be persisted in the database.
     *
     * @param line
     *
     * @return (optional) OptOut
     */
    Optional<OptOut> convert(String line);
}
