package gov.cms.ab2d.worker;

import gov.cms.ab2d.common.model.Job;
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
import java.util.concurrent.locks.Lock;

@Slf4j
@Component
/**
 * This class handles the bulk export requests that Spring Integration streams as messages from the channel.
 * It locks a particular request for processing via a database-centric lock and then kicks off a worker processing.
 */
public class JobHandler implements MessageHandler {

    @Autowired
    /**
     * Export requests must be locked globally to avoid race conditions among workers,
     * which is important in a distributed deployments such as ours.
     */
    private LockRegistry lockRegistry;

    @Autowired
    private WorkerService workerService;

    @Override
    public void handleMessage(Message<?> message) throws MessagingException {

        final String lockKeyId = getLockKey(message);

        final Lock lock = lockRegistry.obtain(lockKeyId);

        // Inability to obtain a lock means other worker is already taking care of the request
        // in which case we do nothing and return.
        if (lock.tryLock()) {
            try {
                workerService.process(lockKeyId);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } finally {
                lock.unlock();
            }
        }
    }

    private String getLockKey(Message<?> message) {
        final Job job = getJob(message);

        final String lockKeyId = String.valueOf(job.getId());
        log.info("================================================================================");
        log.info(" Lock Key based on Job Id from Payload : {} ", lockKeyId);
        log.info("================================================================================");

        return lockKeyId;
    }

    /**
     * Since the jdbcMessageSource fetches only one row at a time,
     * the message will always contain exactly 1 job instance
     *
     * @param message
     * @return
     */
    private Job getJob(Message<?> message) {
        final List<Job> payload = (List<Job>) message.getPayload();
        return payload.get(0);
    }

}
