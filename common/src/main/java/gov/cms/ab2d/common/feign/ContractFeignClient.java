package gov.cms.ab2d.common.feign;

import gov.cms.ab2d.contracts.model.ContractAPI;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Profile;

@FeignClient(name = "contract", url = "${contract.base.url}")
@Profile("prod")
public interface ContractFeignClient extends ContractAPI {
}
