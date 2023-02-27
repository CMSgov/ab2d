package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.model.PdpClient;
import gov.cms.ab2d.common.repository.PdpClientRepository;
import gov.cms.ab2d.contracts.model.Contract;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class ContractServiceStub implements ContractService {
    PdpClientRepository pdpClientRepository;
    private ArrayList<Contract> contractMap;
    static ArrayList<Contract> contractMapOrig;



    public void reset() {
        contractMap.clear();
        contractMap.addAll(contractMapOrig);
    }

    public ContractServiceStub(PdpClientRepository pdpClientRepository) {
        this.pdpClientRepository = pdpClientRepository;
        contractMap = new ArrayList<>();
        contractMapOrig = new ArrayList<>();
        setupContracts();
        reset();
    }

    private void setupContracts() {
        List<PdpClient> pdpClients = pdpClientRepository.findAll();
        pdpClients.sort(Comparator.comparing(PdpClient::getContractId));

        for (int i = 0; i < pdpClients.size(); i++) {
            Contract contract = new Contract("Z100" + (i+1), "Z100" + (i+1), null, null, null);
            contract.setId(pdpClients.get(i).getContractId());
            contract.setAttestedOn(OffsetDateTime.parse( "2020-03-01T12:00-06:00"));
            contract.setUpdateMode(Contract.UpdateMode.NONE);
            contract.setCreated(OffsetDateTime.now());
            contract.setModified(OffsetDateTime.now());
            contractMapOrig.add(contract);
        }
    }

    @Override
    public List<Contract> getAllAttestedContracts() {
        return contractMap;
    }

    @Override
    public Optional<Contract> getContractByContractNumber(String contractNumber) {
        return contractMap.stream().filter(contract -> contract.getContractNumber().equals(contractNumber)).findAny();
    }

    @Override
    public void updateContract(Contract contract) {
        contractMap.stream().filter(contractInList -> contractInList.getId().equals(contract.getId())).findAny().ifPresent(value -> contractMap.remove(value));
        contractMap.add(contract);
    }

    @Override
    public Contract getContractByContractId(Long contractId) {
        return contractMap.stream().filter(contract -> contract.getId().equals(contractId)).findAny().orElse(null);
    }
}
