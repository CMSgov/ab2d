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
 * Splits a contract's beneficiaries into partitions along patient_id boundaries.
 * The partitions are built off patient_id to avoid splitting patients between partitions if
 * they are on the boundary.
 */
@Slf4j
public class BeneficiaryPartitioner implements Partitioner, PartitionNameProvider {

    static final String KEY_CONTRACT = "contractNumber";
    static final String KEY_PARTITION_INDEX = "partitionIndex";
    static final String KEY_START_PATIENT = "startPatientId"; // exclusive lower bound
    static final String KEY_END_PATIENT = "endPatientId";     // inclusive upper bound

    static final long UNBOUNDED_START = Long.MIN_VALUE;
    static final long UNBOUNDED_END = Long.MAX_VALUE;

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
        List<Long> upperBounds = computeUpperBounds();

        Map<String, ExecutionContext> partitions = new HashMap<>();
        if (upperBounds == null) {
            log.info("no patients found");
            return partitions;
        }

        int numPartitions = upperBounds.size() + 1;
        long lower = UNBOUNDED_START;
        for (int i = 0; i < numPartitions; i++) {
            long upper = i < upperBounds.size() ? upperBounds.get(i) : UNBOUNDED_END;

            ExecutionContext ec = new ExecutionContext();
            ec.putString(KEY_CONTRACT, contractNumber);
            ec.putInt(KEY_PARTITION_INDEX, i);
            ec.putLong(KEY_START_PATIENT, lower);
            ec.putLong(KEY_END_PATIENT, upper);

            partitions.put(partitionName(i), ec);
            lower = upper;
        }

        log.info("made {} partitions", numPartitions);
        return partitions;
    }

    /**
     * Names of the partitions to reload on restart
     */
    @Override
    public Collection<String> getPartitionNames(int gridSize) {
        List<Long> upperBounds = computeUpperBounds();
        if (upperBounds == null) {
            return List.of();
        }
        int numPartitions = upperBounds.size() + 1;

        List<String> names = new ArrayList<>(numPartitions);
        for (int i = 0; i < numPartitions; i++) {
            names.add(partitionName(i));
        }
        log.info("resuming {} partitions", numPartitions);
        return names;
    }

    /**
     * returns a list of partition boundaries
     * returns null if theres no coverage data and empty list if
     * there is only enough patients for 1 partition
     */
    private List<Long> computeUpperBounds() {
        long maxRow = coverageV3Service.getMaxRowNumber(contractNumber);
        if (maxRow <= 0) {
            return null;
        }
        int size = partitionSize <= 0 ? (int) Math.min(maxRow, Integer.MAX_VALUE) : partitionSize;
        return coverageV3Service.getPartitionBoundaryPatientIds(contractNumber, size);
    }

    private static String partitionName(int index) {
        return "partition" + index;
    }
}
