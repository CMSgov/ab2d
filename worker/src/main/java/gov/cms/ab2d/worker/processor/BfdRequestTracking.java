package gov.cms.ab2d.worker.processor;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Getter
@Slf4j
public class BfdRequestTracking {

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
	private final List<Double> requestEOBFromServerTimes;
	private final List<Double> requestNextBundleFromServerTimes;

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
		val start = LocalDateTime.now();
		val result = supplier.get();
		val end = LocalDateTime.now();
		val duration = ChronoUnit.MILLIS.between(start, end);
		val durationSeconds = duration / 1000.0;
		if (type == BfdRequestType.REQUEST_EOB) {
			requestEOBFromServerTimes.add(durationSeconds);
		} else if (type == BfdRequestType.REQUEST_NEXT_BUNDLE) {
			requestNextBundleFromServerTimes.add(durationSeconds);
		}
		return result;
	}

	public void summarizeResponseTimes() {
		summarizeResponseTimes("requestEOBFromServer", requestEOBFromServerTimes);
		summarizeResponseTimes("requestNextBundleFromServer", requestNextBundleFromServerTimes);
	}

	private void summarizeResponseTimes(String bfdRequestOperation, List<Double> requestTimes) {
		if (requestTimes.isEmpty()) {
			return;
		}
		val stats = requestTimes.stream().collect(Collectors.summarizingDouble(Double::doubleValue));
		log.info("BFD {} stats; Job={}; Num requests={}; Average={}s, Min={}s, Max={}s",
			bfdRequestOperation,
			jobUuid,
			stats.getCount(),
			stats.getAverage(),
			stats.getMin(),
			stats.getMax()
		);
	}
}
