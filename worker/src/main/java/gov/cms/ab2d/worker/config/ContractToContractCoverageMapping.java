package gov.cms.ab2d.worker.config;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.coverage.model.ContractForCoverageDTO;
import gov.cms.ab2d.worker.model.ContractWorker;
import gov.cms.ab2d.worker.model.ContractWorkerEntity;
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

        Converter<ContractWorker, ContractForCoverageDTO> coverageContractDTOConverter = new AbstractConverter<>() {

            @Override
            protected ContractForCoverageDTO convert(ContractWorker source) {
                return new ContractForCoverageDTO(source.getContractNumber(), source.getAttestedOn(), ContractForCoverageDTO.ContractType.valueOf(source.getContractType().toString())); //NOSONAR
            }
        };

        Converter<Contract, ContractForCoverageDTO> contractToConverageDto = new AbstractConverter<>() {

            @Override
            protected ContractForCoverageDTO convert(Contract source) {
                return new ContractForCoverageDTO(source.getContractNumber(), source.getAttestedOn(), ContractForCoverageDTO.ContractType.valueOf(source.getContractType().toString())); //NOSONAR
            }
        };

        Converter<Contract, ContractWorker> contractToWorkerDto = new AbstractConverter<>() {
            @Override
            protected ContractWorker convert(Contract source) {
                return new ContractWorkerEntity(source.getId(), source.getContractNumber(), source.getContractName(), ContractWorker.UpdateMode.valueOf(source.getUpdateMode().toString()), ContractWorker.ContractType.valueOf(source.getContractType().toString()), source.getAttestedOn()); //NOSONAR
            }
        };


        modelMapper.addConverter(coverageContractDTOConverter);
        modelMapper.addConverter(contractToConverageDto);
        modelMapper.addConverter(contractToWorkerDto);


    }

    public ContractForCoverageDTO map(ContractWorker contract) {
        return modelMapper.map(contract, ContractForCoverageDTO.class);
    }

    public ContractForCoverageDTO map(Contract contract) {
        return modelMapper.map(contract, ContractForCoverageDTO.class);
    }

    public ContractWorker mapWorkerDto(Contract contract) {
        return modelMapper.map(contract, ContractWorker.class);
    }
}
