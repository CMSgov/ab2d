package gov.cms.ab2d.worker.processor.stub;

import gov.cms.ab2d.worker.config.RoundRobinThreadPoolTaskExecutor;
import gov.cms.ab2d.worker.config.WorkerConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.Future;

@Slf4j
@Component
public class ProcessThread {
    @Autowired
    private WorkerConfig workerConfig;

    public Future<?> sleep(String contract) {
        RoundRobinThreadPoolTaskExecutor taskExecutor = (RoundRobinThreadPoolTaskExecutor) workerConfig.patientProcessorThreadPool();
        return (taskExecutor.submitWithCategory(contract, () -> call(contract)));
    }

    public String call(String contract) {
        try {
            //log.info(contract + " - " + Thread.currentThread().getId() + " Started");
            Thread.sleep(300);
            return Thread.currentThread() + " Done";
        } catch (Exception ex) {
            return Thread.currentThread() + " Error";
        }
    }
}
