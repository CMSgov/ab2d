package gov.cms.ab2d.worker.config;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.coverage.model.ContractForCoverageDTO;
import javax.annotation.PostConstruct;
import org.modelmapper.AbstractConverter;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class ContractToContractCoverageMapping {

    private static ModelMapper modelMapper;

    public ContractToContractCoverageMapping() {
        init();
    }


    @PostConstruct
    public static void init() {
        modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setSkipNullEnabled(true);

        Converter<Contract, ContractForCoverageDTO> coverageContractDTOConverter = new AbstractConverter<>() {

            @Override
            protected ContractForCoverageDTO convert(Contract source) {
                return new ContractForCoverageDTO(source.getContractNumber(), source.getAttestedOn(), ContractForCoverageDTO.ContractType.valueOf(source.getContractType().toString())); //NOSONAR
            }
        };

        modelMapper.addConverter(coverageContractDTOConverter);
    }

    public ContractForCoverageDTO map(Contract contract) {
        return modelMapper.map(contract, ContractForCoverageDTO.class);
    }
}