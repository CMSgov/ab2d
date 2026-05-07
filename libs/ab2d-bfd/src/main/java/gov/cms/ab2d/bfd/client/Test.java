package gov.cms.ab2d.bfd.client;

import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpGet;

import java.time.OffsetDateTime;
import java.util.List;

public class Test {

	public static void main(String[] args) {

		long patientId = 1234L;
		OffsetDateTime since = OffsetDateTime.now().minusDays(4).minusMonths(1);
		OffsetDateTime until = OffsetDateTime.now().minusDays(4);
		List<String> serviceDates = List.of();
		int pageSize = 10;


		String urlLocation = "XYZ";
		StringBuilder url = new StringBuilder(urlLocation + "ExplanationOfBenefit?patient=" + patientId);

		if (true) {
			url.append("&_source=NCH");
			url.append("&_security:not=42CFRPart2");
		}
		else {
			url.append("&excludeSAMHSA=true");
			url.append("&type=carrier,dme,hha,hospice,inpatient,outpatient,snf");
		}

		if (since != null) {
			url.append("&_lastUpdated=ge").append(since);
		}

		if (until != null) {
			url.append("&_lastUpdated=le").append(until);
		}

		if (serviceDates != null) {
			for (String serviceDate : serviceDates) {
				url.append("&service-date=").append(serviceDate);
			}
		}

		if (pageSize > 0) {
			url.append("&_count=").append(pageSize);
		}

		HttpGet request = new HttpGet(url.toString());


		request.addHeader(HttpHeaders.ACCEPT, "gzip");
		request.addHeader(HttpHeaders.ACCEPT_CHARSET, "utf-8");
//        request.addHeader(BFDClient.BFD_HDR_BULK_CLIENTID, contractNum);
//        request.addHeader(BFDClient.BFD_HDR_BULK_JOBID, bulkJobId);

		System.out.println(request.toString());

	}

}
