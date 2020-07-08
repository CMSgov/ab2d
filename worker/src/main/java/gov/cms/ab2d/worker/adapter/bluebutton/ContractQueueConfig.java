package gov.cms.ab2d.worker.adapter.bluebutton;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ContractQueueConfig {
    @Bean(name = "patientContractThreadPool")
    public ThreadPoolTaskExecutor patientContractThreadPool() {
        final ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(6);
        taskExecutor.setMaxPoolSize(12);
        taskExecutor.setThreadNamePrefix("contractp-");
        return taskExecutor;
    }
}
