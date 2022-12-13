package gov.cms.ab2d.common.feign;

import gov.cms.ab2d.contracts.model.ContractAPI;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "contract", url = "${contract.base.url}")
public interface ContractFeignClient extends ContractAPI {
}
