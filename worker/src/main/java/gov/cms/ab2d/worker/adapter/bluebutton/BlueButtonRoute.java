package gov.cms.ab2d.worker.adapter.bluebutton;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bb")
public class BlueButtonRoute {

    private final BeneficiaryAdapter beneficiaryAdapter;

    public BlueButtonRoute(BeneficiaryAdapter beneficiaryAdapter) {
        this.beneficiaryAdapter = beneficiaryAdapter;
    }

    @GetMapping("/v1/contract/{contractNumber}/patients")
    public ResponseEntity<GetPatientsByContractResponse> get(@PathVariable String contractNumber) {
        final var patientsByContract = beneficiaryAdapter.getPatientsByContract(contractNumber);
        return ResponseEntity.ok(patientsByContract);
    }
}
