package gov.cms.ab2d.contracts.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
@Slf4j
public class HealthcheckServiceImpl implements HealthcheckService {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public boolean checkDatabaseConnection() {
        try {
            jdbcTemplate.query("SELECT 1", x -> {
            });
        } catch (Exception e) {
            log.error("Healthcheck failed", e);
            return false;
        }
        return true;
    }
}
