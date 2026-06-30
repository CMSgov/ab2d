package gov.cms.ab2d.worker.processor.prototype;

import gov.cms.ab2d.coverage.service.v3.CoverageV3Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.partition.Partitioner;
import org.springframework.batch.infrastructure.item.ExecutionContext;

import java.util.HashMap;
import java.util.Map;

/**
 * splits contract benes into partitions
 */
@Slf4j
public class BeneficiaryPartitioner implements Partitioner {

    static final String KEY_CONTRACT = "contractNumber";
    static final String KEY_PARTITION_INDEX = "partitionIndex";
    static final String KEY_START_ROW = "startRow";
    static final String KEY_END_ROW = "endRow";
    static final String KEY_BENES = "benesInPartition";

    private final CoverageV3Service coverageV3Service;
    private final String contractNumber;
    private final int partitionSize;

    public BeneficiaryPartitioner(CoverageV3Service coverageV3Service, String contractNumber, int partitionSize) {
        this.coverageV3Service = coverageV3Service;
        this.contractNumber = contractNumber;
        this.partitionSize = partitionSize;
    }

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        int total = coverageV3Service.getDistinctPatientCount(contractNumber);

        Map<String, ExecutionContext> partitions = new HashMap<>();
        if (total <= 0) {
            log.info("no patients found");
            return partitions;
        }

        int size = partitionSize <= 0 ? total : partitionSize;
        int numPartitions = (total + size - 1) / size; // ceil(total / size)

        for (int i = 0; i < numPartitions; i++) {
            long startRow = (long) i * size + 1;
            long endRow = Math.min((long) (i + 1) * size, total);

            ExecutionContext ec = new ExecutionContext();
            ec.putString(KEY_CONTRACT, contractNumber);
            ec.putInt(KEY_PARTITION_INDEX, i);
            ec.putLong(KEY_START_ROW, startRow);
            ec.putLong(KEY_END_ROW, endRow);
            ec.putInt(KEY_BENES, (int) (endRow - startRow + 1));

            partitions.put("partition" + i, ec);
        }

        log.info("made {} partitions", numPartitions);
        return partitions;
    }
}
