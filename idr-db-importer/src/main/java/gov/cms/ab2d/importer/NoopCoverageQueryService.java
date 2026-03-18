package gov.cms.ab2d.importer;


import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;

@Component
@Profile({"dev", "test", "sandbox"})
public class NoopCoverageQueryService implements CoverageQueryService {

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public Connection open() {
        return null;
    }

    @Override
    public PreparedStatement prepare(Connection connection) {
        return null;
    }
}