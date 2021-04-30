package gov.cms.ab2d.hpms.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Service
public class AirtableFetcherUpdaterImpl implements AirtableFetcherUpdater {

    private static final String BASE_URI = "https://api.airtable.com/v0/appXQTP0XARA6P5xy/Mother";

    @Override
    public void fetchContracts(Consumer<ObjectNode> contractCallback) {
        Flux<ObjectNode> orgInfoFlux = WebClient.create()
                .get().uri(BASE_URI)
                .headers(this::buildAuthHeaders)
                .retrieve()
                .bodyToFlux(ObjectNode.class);

        orgInfoFlux.subscribe(contractCallback);
    }

    public void buildAuthHeaders(HttpHeaders headers) {
        headers.set(AUTHORIZATION, "Bearer keyxIGvKaCUgcz6qj");
    }
}
