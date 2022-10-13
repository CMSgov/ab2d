package gov.cms.ab2d.eventlogger.reports.sql;

import gov.cms.ab2d.eventclient.events.LoggableEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LoggerEventRepository {
    private final JdbcTemplate template;

    public LoggerEventRepository(JdbcTemplate template) {
        this.template = template;
    }

    public List<LoggableEvent> load(Class eventClass) {
//        String qry = "SELECT * FROM " + configMapper.getTableMapper(eventClass) + " ORDER BY id";
//        return template.query(qry, configMapper.getMapper(eventClass));
        return new ArrayList<>();
    }

    public List<LoggableEvent> load(Class eventClass, String jobId) {
//        String qry = "SELECT * FROM " + configMapper.getTableMapper(eventClass) + " WHERE job_id = ? ORDER BY id";
//        return template.query(qry, new Object[]{jobId}, configMapper.getMapper(eventClass));
        return new ArrayList<>();
    }

    public List<LoggableEvent> load() {
//        Set<Class<? extends LoggableEvent>> entries = configMapper.getClasses();
        List<LoggableEvent> allEvents = new ArrayList<>();
//        entries.forEach(c -> allEvents.addAll(load(c)));
        return allEvents;
    }

    public void delete() {
//        Set<Class<? extends LoggableEvent>> entries = configMapper.getClasses();
//        entries.forEach(c -> delete(c));
    }

    public void delete(Class eventClass) {
//        String qry = "DELETE FROM " + configMapper.getTableMapper(eventClass);
//        template.update(qry);
    }
}
