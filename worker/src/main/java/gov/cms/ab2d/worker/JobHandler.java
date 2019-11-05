package gov.cms.ab2d.worker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;


/**
 * This class handles the bulk export requests that Spring Integration streams as messages from the channel.
 * It locks a particular request for processing via a database-centric lock and then kicks off a worker processing.
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

        final Lock lock = lockRegistry.obtain(jobId);

        // Inability to obtain a lock means other worker is already taking care of the request
        // in which case we do nothing and return.
        if (lock.tryLock()) {
            try {
                workerService.process(jobId);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } finally {
                lock.unlock();
            }
        } else {
            log.info("===============================================");
            log.info("Could not get a lock for Job : [{}] ", jobId);
            log.info("===============================================");
        }
    }

    private String getJobId(Message<?> message) {
        final List<Map<String, Long>> payload = (List<Map<String, Long>>) message.getPayload();
        long jobId = payload.get(0).get("id");
        return String.valueOf(jobId);
    }

}
