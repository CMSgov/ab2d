package gov.cms.ab2d.hpms.service;


import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.function.Consumer;

public interface AirtableFetcherUpdater {

    void fetchContracts(Consumer<ObjectNode> contractCallback);
}
