package gov.cms.ab2d.worker.config;

import gov.cms.ab2d.worker.service.WorkerService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import static gov.cms.ab2d.common.util.Constants.JOB_LOG;


/**
 * This handler gets triggered when a job is submitted into the job table.
 * Spring Integration polls the jobs table in the database
 * And when a new record is inserted into the jobs table, Spring Integration streams into the subscribable executor channel.
 * It locks the job for processing via a database-centric lock and then delegates to a service for processing.
 */
@Slf4j
@Component
public class JobHandler implements MessageHandler {

    /**
     * Export requests must be locked globally to avoid race conditions among workers,
     * which is important in a distributed deployments such as ours.
     */
    @Autowired
    private LockRegistry lockRegistry;

    @Autowired
    private WorkerService workerService;


    @Override
    public void handleMessage(Message<?> message) throws MessagingException {

        final String jobId = getJobId(message);

        MDC.put(JOB_LOG, jobId);

        final Lock lock = lockRegistry.obtain(jobId);

        // Inability to obtain a lock means other worker is already taking care of the request
        // in which case we do nothing and return.
        if (lock.tryLock()) {
            try {
                workerService.process(jobId);
            } finally {
                lock.unlock();
            }
        }

        MDC.remove(JOB_LOG);
    }

    private String getJobId(Message<?> message) {
        final List<Map<String, Object>> payload = (List<Map<String, Object>>) message.getPayload();
        final Map<String, Object> row0 = payload.get(0);
        return String.valueOf(row0.get("job_uuid"));
    }

}
