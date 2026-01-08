package gov.cms.ab2d.importer;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@Slf4j
@RequiredArgsConstructor
@EnableRetry
public class SpringBootApp {

	private final CoverageV3S3Importer importer;

	public static void main(String[] args) {
		SpringApplication.run(SpringBootApp.class, args);
	}

	@PostConstruct
	public void runAfterStartup() {
		log.info("Starting coverage_v3 S3 -> Aurora import task");
		int exitCode = 1;
		try {
			importer.runOnce();
			exitCode = 0;
			log.info("Import task completed successfully");
		} catch (Exception e) {
			log.error("Import task failed", e);
		} finally {
			System.exit(exitCode);
		}
	}
}

