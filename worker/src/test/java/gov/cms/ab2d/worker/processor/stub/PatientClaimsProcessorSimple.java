package gov.cms.ab2d.worker.processor.stub;

import gov.cms.ab2d.worker.adapter.bluebutton.GetPatientsByContractResponse;
import gov.cms.ab2d.worker.processor.PatientClaimsProcessor;
import gov.cms.ab2d.worker.processor.StreamHelper;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.OffsetDateTime;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

@Slf4j
public class PatientClaimsProcessorSimple implements PatientClaimsProcessor {
    @Setter
    private Executor threadPoolExecutor;

    private class TestThread implements Callable {

        private String threadName;
        TestThread(String threadName) {
            this.threadName = threadName;
        }

        @Override
        public Object call() throws Exception {
            Thread.sleep(200);
            System.out.println("Thread for patient " + threadName + " is done");
            return null;
        }
    }

    public void setExecutor(Executor executor) {
        this.threadPoolExecutor = executor;
    }

    @Override
    public Future<Void> process(GetPatientsByContractResponse.PatientDTO patientDTO, StreamHelper writer, OffsetDateTime attTime) {
        try {
            TestThread th = new TestThread(patientDTO.getPatientId());
            return (Future<Void>) ((ThreadPoolTaskExecutor) threadPoolExecutor).submit(th);
        } catch (Exception ex) {
            log.error("Error on patient {}", patientDTO.getPatientId());
            return null;
        }
    }
}