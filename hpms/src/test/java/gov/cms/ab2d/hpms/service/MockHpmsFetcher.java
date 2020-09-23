package gov.cms.ab2d.hpms.service;

import gov.cms.ab2d.hpms.hmsapi.HPMSAttestation;
import gov.cms.ab2d.hpms.hmsapi.HPMSAttestationsHolder;
import gov.cms.ab2d.hpms.hmsapi.HPMSOrganizationInfo;
import gov.cms.ab2d.hpms.hmsapi.HPMSOrganizations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class MockHpmsFetcher implements HPMSFetcher {

    private static final List<HPMSOrganizationInfo> orgSet = new ArrayList<>();

    private static final Map<String, HPMSAttestation> attests = new HashMap<>();

    static {
        attests.put("S1234", new HPMSAttestation("S1234", true, "01/31/2020"));
        attests.put("S2341", new HPMSAttestation("S1234", true, "02/13/2020"));
        attests.put("S3412", new HPMSAttestation("S1234", true, "03/24/2020"));
        attests.put("S4123", new HPMSAttestation("S1234", true, "04/15/2020"));

        HPMSOrganizationInfo orgInfo = new HPMSOrganizationInfo("ABC Org",
                5, "S1234", "Contract ABC", "ABC Marketing");
        orgSet.add(orgInfo);
        orgInfo = new HPMSOrganizationInfo("NBC Org",
                6, "S2341", "Contract NBC", "NBC Marketing");
        orgSet.add(orgInfo);
        orgInfo = new HPMSOrganizationInfo("CBS Org",
                7, "S3412", "Contract CBS", "CBS Marketing");
        orgSet.add(orgInfo);
        orgInfo = new HPMSOrganizationInfo("TNT Org",
                8, "S4123", "Contract TNT", "TNT Marketing");
        orgSet.add(orgInfo);
    }

    @Override
    public void retrieveSponsorInfo(Consumer<HPMSOrganizations> hpmsOrgCallback) {
        hpmsOrgCallback.accept(new HPMSOrganizations(orgSet));
    }

    @Override
    public void retrieveAttestationInfo(Consumer<HPMSAttestationsHolder> hpmsAttestationCallback, String jsonContractIds) {
        String[] contractIds = jsonContractIds.replaceAll("[^,a-zA-Z0-9]","").split(",");
        Set<HPMSAttestation> retAttests = new HashSet<>();
        for (String contractId : contractIds) {
            if (!attests.containsKey(contractId)) {
                throw new IllegalArgumentException(contractId + " not found in test data.");
            }
            retAttests.add(attests.get(contractId));
        }
        hpmsAttestationCallback.accept(new HPMSAttestationsHolder(retAttests));
    }
}
