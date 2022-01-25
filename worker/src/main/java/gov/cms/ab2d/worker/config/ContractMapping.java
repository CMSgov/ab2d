package gov.cms.ab2d.worker.config;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.coverage.model.CoverageContractDTO;
import javax.annotation.PostConstruct;
import org.modelmapper.AbstractConverter;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class ContractMapping {

    private static ModelMapper modelMapper;


    @PostConstruct
    public static void init() {
        modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setSkipNullEnabled(true);

        Converter<Contract, CoverageContractDTO> coverageContractDTOConverter = new AbstractConverter<>() {

            @Override
            protected CoverageContractDTO convert(Contract source) {
                return new CoverageContractDTO(source.getContractNumber(), source.getAttestedOn()); //NOSONAR
            }
        };

        modelMapper.addConverter(coverageContractDTOConverter);
    }

    public ModelMapper getModelMapper() {
        return modelMapper;
    }

    public CoverageContractDTO map(Contract contract) {
        return modelMapper.map(contract, CoverageContractDTO.class);
    }
}
