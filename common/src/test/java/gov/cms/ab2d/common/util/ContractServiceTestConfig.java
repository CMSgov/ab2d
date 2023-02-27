package gov.cms.ab2d.common.util;

import gov.cms.ab2d.common.feign.ContractFeignClient;
import gov.cms.ab2d.common.repository.PdpClientRepository;
import gov.cms.ab2d.common.service.ContractService;
import gov.cms.ab2d.common.service.ContractServiceStub;
import gov.cms.ab2d.contracts.model.ContractDTO;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@TestConfiguration
public class ContractServiceTestConfig {
    @Autowired
    PdpClientRepository pdpClientRepository;

    @Bean
    @Primary
    public ContractService contractServiceTest() {
        return new ContractServiceStub(pdpClientRepository);
    }

    @Bean
    public ContractServiceStub contractServiceStub(ContractService contractService) {
        return (ContractServiceStub) contractService;
    }

    @Bean
    @Primary
    @Profile("test")
    public ContractFeignClient contractFeignClientMock() {
        return new ContractFeignClient() {
            @Override
            public List<ContractDTO> getContracts(Long aLong) {
                return null;
            }

            @Override
            public void updateContract(ContractDTO contractDTO) {

            }

            @Override
            public ContractDTO getContractByNumber(String s) {
                return null;
            }
        };
    }

}
