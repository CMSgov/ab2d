package gov.cms.ab2d.importer;

import java.sql.Connection;
import java.sql.PreparedStatement;

public interface CoverageQueryService {

    boolean isEnabled();

    Connection open() throws Exception;

    PreparedStatement prepare(Connection connection) throws Exception;
}