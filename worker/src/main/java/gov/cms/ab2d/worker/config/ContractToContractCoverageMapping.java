package gov.cms.ab2d.worker.config;

import gov.cms.ab2d.common.dto.ContractDTO;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.coverage.model.ContractForCoverageDTO;
import javax.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;
import org.modelmapper.AbstractConverter;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ContractToContractCoverageMapping {

    private static ModelMapper modelMapper;

    public ContractToContractCoverageMapping() {
        init();
    }


    @PostConstruct
    public static void init() {
        modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setSkipNullEnabled(true);

        Converter<ContractDTO, ContractForCoverageDTO> coverageContractDTOConverter = new AbstractConverter<>() {

            @Override
            protected ContractForCoverageDTO convert(ContractDTO source) {
                return new ContractForCoverageDTO(source.getContractNumber(), source.getAttestedOn(), ContractForCoverageDTO.ContractType.valueOf(source.getContractType().toString())); //NOSONAR
            }
        };

        Converter<Contract, ContractForCoverageDTO> contractToConverageDto = new AbstractConverter<>() {

            @Override
            protected ContractForCoverageDTO convert(Contract source) {
                return new ContractForCoverageDTO(source.getContractNumber(), source.getAttestedOn(), ContractForCoverageDTO.ContractType.valueOf(source.getContractType().toString())); //NOSONAR
            }
        };

        modelMapper.addConverter(coverageContractDTOConverter);
        modelMapper.addConverter(contractToConverageDto);
    }

    public ContractForCoverageDTO map(ContractDTO contract) {
        return modelMapper.map(contract, ContractForCoverageDTO.class);
    }

    public ContractForCoverageDTO map(Contract contract) {
        ContractForCoverageDTO contractForCoverageDTO = modelMapper.map(contract, ContractForCoverageDTO.class);
        log.info("Attested from DB {} VS mapped {}", contract.getAttestedOn(), contractForCoverageDTO.getAttestedOn());
        return contractForCoverageDTO;
    }
}
