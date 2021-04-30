package gov.cms.ab2d.hpms.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.repository.ContractRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AirtableUpdaterServiceImpl implements AirtableUpdaterService {

    private final AirtableFetcherUpdater fetcher;

    private final ContractRepository contractRepository;

    private final ObjectMapper mapper;

    private final ObjectNode oobDisableFields;

    public AirtableUpdaterServiceImpl(AirtableFetcherUpdater fetcher, ContractRepository contractRepository) {
        this.fetcher = fetcher;
        this.contractRepository = contractRepository;
        mapper = new ObjectMapper();
        oobDisableFields = mapper.createObjectNode();
        oobDisableFields.put("update_mode", Contract.UpdateMode.TEST.name());
    }

    @Override
    public void updateContracts() {
        fetcher.fetchContracts(this::processContracts);

        throw new UnsupportedOperationException("Implement ME!!!");
    }

    private void processContracts(ObjectNode contracts) {

        ArrayNode contractArray = contracts.withArray("records");
        Map<String, ObjectNode> atContracts = new HashMap<>(89);
        contractArray.spliterator()
                .forEachRemaining(contract -> atContracts.put(extractContractNumber(contract), (ObjectNode) contract));

        List<Contract> ab2dContracts = contractRepository.findAll();
        Set<String> currentContracts = ab2dContracts.stream()
                .map(Contract::getContractNumber).collect(Collectors.toSet());

        List<Contract> newContracts = ab2dContracts.stream()
                .filter(contract -> !atContracts.containsKey(contract.getContractNumber()) && contract.isAutoUpdateable())
                .collect(Collectors.toList());
        addContracts(newContracts);

        ArrayNode recordsToUpdate = new ObjectMapper().createArrayNode();
        List<ObjectNode> oobContracts = atContracts.values().stream()
                .filter(missing -> orphan(currentContracts, missing)).collect(Collectors.toList());
        if (!oobContracts.isEmpty()) {
            disableOOB(recordsToUpdate, oobContracts);
        }

        ab2dContracts.forEach(contract -> addUpdatesToContract(recordsToUpdate, contract, atContracts.get(contract.getContractNumber())));
    }

    private void addUpdatesToContract(ArrayNode recordsToUpdate, Contract contract, ObjectNode motherContract) {
        String contractNameVal = retrieveStringValue(motherContract, "Contract Name");
        long parentOrgId = Long.parseLong(retrieveStringValue(motherContract, "PDP Org"));
        String parentOrgName = retrieveStringValue(motherContract, "Parent Org Name");
        String orgMarketingName = retrieveStringValue(motherContract, "Org Marketing Name");

        if (!contract.hasChanges(contractNameVal, parentOrgId, parentOrgName, orgMarketingName)) {
            // Nothing to do here
            return;
        }


            ObjectNode updateMe = null;
//        if ()
    }

    private void disableOOB(ArrayNode recordsToUpdate, List<ObjectNode> oobContracts) {
        oobContracts.forEach(objectNode -> addToRecords(recordsToUpdate, objectNode));
    }

    private void addToRecords(ArrayNode recordsToUpdate, JsonNode jsonNode) {
        final String ID_KEY = "id";
        ObjectNode nodeToUpdate = mapper.createObjectNode();
        nodeToUpdate.put(ID_KEY, jsonNode.get(ID_KEY).asText());
        nodeToUpdate.set("fields", oobDisableFields);
        recordsToUpdate.add(nodeToUpdate);
    }

    private void addContracts(List<Contract> newContracts) {
        throw new UnsupportedOperationException("Implement Me!");
    }

    private boolean orphan(Set<String> persistedContracts, ObjectNode candidate) {
        return isAutomatic(candidate) && !persistedContracts.contains(extractContractNumber(candidate));

    }

    private boolean isAutomatic(ObjectNode entry) {
        return Contract.UpdateMode.AUTOMATIC.name().equals(retrieveStringValue(entry, "update_mode"));
    }

    private String retrieveStringValue(ObjectNode entry, String fieldName) {
        return entry.get("fields").get(fieldName).asText();
    }


    private String extractContractNumber(JsonNode jsonNode) {
        return jsonNode.get("fields").get("Contract #").asText();
    }
}
