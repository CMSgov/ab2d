package gov.cms.ab2d.worker.properties;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Properties;

public class CustomProperties extends Properties {

    public CustomProperties() {
        super();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        List<Map<String, Object>> configs = jdbcTemplate
                .queryForList("select config_key, config_value from config_params");

        for (Map<String, Object> config : configs) {
            setProperty((config.get("config_key")).toString(), (config.get("config_value")).toString());
        }
    }
}
