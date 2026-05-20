package gov.cms.ab2d.worker.processor;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Getter
@Slf4j
public class BfdRequestTracking {

    // Instance used if BFD request tracking is disabled
    public static final BfdRequestTracking NOOP = new BfdRequestTracking() {
        @Override
        public <T> T executeRequest(BfdRequestType type, Supplier<T> supplier) {
            return supplier.get();
        }

        @Override
        public void summarizeResponseTimes() {
            // do nothing
        }
    };

    public enum BfdRequestType {
        REQUEST_EOB,
        REQUEST_NEXT_BUNDLE
    }

    private final String jobUuid;
    private final List<Long> requestEOBFromServerTimes;
    private final List<Long> requestNextBundleFromServerTimes;

    private BfdRequestTracking() {
        this.jobUuid = null;
        this.requestEOBFromServerTimes = null;
        this.requestNextBundleFromServerTimes = null;
    }

    public BfdRequestTracking(final String jobUuuid) {
        this.jobUuid = jobUuuid;
        this.requestEOBFromServerTimes = new ArrayList<>(100);
        this.requestNextBundleFromServerTimes = new ArrayList<>();
    }

    public <T> T executeRequest(BfdRequestType type, Supplier<T> supplier) {
        val start = System.nanoTime();
        val result = supplier.get();
        val end = System.nanoTime();
        val durationMs = (end - start) / 1_000_000;
        if (type == BfdRequestType.REQUEST_EOB) {
            requestEOBFromServerTimes.add(durationMs);
        } else if (type == BfdRequestType.REQUEST_NEXT_BUNDLE) {
            requestNextBundleFromServerTimes.add(durationMs);
        }
        return result;
    }

    public void summarizeResponseTimes() {
        summarizeResponseTimes("requestEOBFromServer", requestEOBFromServerTimes);
        summarizeResponseTimes("requestNextBundleFromServer", requestNextBundleFromServerTimes);
    }

    private void summarizeResponseTimes(String bfdRequestOperation, List<Long> requestTimes) {
        if (requestTimes.isEmpty()) {
            return;
        }
        val stats = requestTimes.stream().collect(Collectors.summarizingLong(Long::longValue));
        log.info("BFD {} stats; Job={}; Num requests={}; Average={}ms; Median={}ms; Min={}ms; Max={}ms",
            bfdRequestOperation,
            jobUuid,
            stats.getCount(),
            stats.getAverage(),
            getMedian(requestTimes),
            stats.getMin(),
            stats.getMax()
        );
    }

    private static double getMedian(List<Long> times) {
        Collections.sort(times);
        int size = times.size();
        if (size % 2 != 0) {
            return times.get(size / 2);
        } else {
            long mid1 = times.get(size / 2 - 1);
            long mid2 = times.get(size / 2);
            return (mid1 + mid2) / 2.0;
        }

    }

}
