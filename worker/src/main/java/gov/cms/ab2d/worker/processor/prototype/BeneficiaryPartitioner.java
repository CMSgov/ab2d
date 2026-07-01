package gov.cms.ab2d.worker.processor.prototype;

import gov.cms.ab2d.coverage.service.v3.CoverageV3Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.partition.PartitionNameProvider;
import org.springframework.batch.core.partition.Partitioner;
import org.springframework.batch.infrastructure.item.ExecutionContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Splits contract benes into partitions.
 * On restart, it reuses the checked in partitions.
 */
@Slf4j
public class BeneficiaryPartitioner implements Partitioner, PartitionNameProvider {

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
        int numPartitions = numPartitions(total, size);

        for (int i = 0; i < numPartitions; i++) {
            long startRow = (long) i * size + 1;
            long endRow = Math.min((long) (i + 1) * size, total);

            ExecutionContext ec = new ExecutionContext();
            ec.putString(KEY_CONTRACT, contractNumber);
            ec.putInt(KEY_PARTITION_INDEX, i);
            ec.putLong(KEY_START_ROW, startRow);
            ec.putLong(KEY_END_ROW, endRow);
            ec.putInt(KEY_BENES, (int) (endRow - startRow + 1));

            partitions.put(partitionName(i), ec);
        }

        log.info("made {} partitions", numPartitions);
        return partitions;
    }

    /**
     * Grab the partitions from context
     */
    @Override
    public Collection<String> getPartitionNames(int gridSize) {
        int total = coverageV3Service.getDistinctPatientCount(contractNumber);
        if (total <= 0) {
            return List.of();
        }
        int size = partitionSize <= 0 ? total : partitionSize;
        int numPartitions = numPartitions(total, size);

        List<String> names = new ArrayList<>(numPartitions);
        for (int i = 0; i < numPartitions; i++) {
            names.add(partitionName(i));
        }
        log.info("resuming {} partitions", numPartitions);
        return names;
    }

    private static int numPartitions(int total, int size) {
        return (total + size - 1) / size;
    }

    private static String partitionName(int index) {
        return "partition" + index;
    }
}
