package gov.cms.ab2d.hpms.service;

import gov.cms.ab2d.hpms.hmsapi.HPMSAttestation;
import gov.cms.ab2d.hpms.hmsapi.HPMSOrganizationInfo;

import java.util.*;
import java.util.function.Consumer;

public class MockHpmsFetcher implements HPMSFetcher {

    private static final List<HPMSOrganizationInfo> orgSet = new ArrayList<>();

    private static final Map<String, HPMSAttestation> attests = new HashMap<>();

    static {
        attests.put("S1234", new HPMSAttestation("S1234", true, "2020-01-31 14:57:34"));
        attests.put("S2341", new HPMSAttestation("S2341", true, "2020-02-13 14:57:34"));
        attests.put("S3412", new HPMSAttestation("S3412", true, "2020-03-24 14:57:34"));
        attests.put("S4123", new HPMSAttestation("S4123", true, "2020-04-15 14:57:34"));

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
    public void retrieveSponsorInfo(Consumer<List<HPMSOrganizationInfo>> hpmsOrgCallback) {
        hpmsOrgCallback.accept(orgSet);
    }

    @Override
    public void retrieveAttestationInfo(Consumer<Set<HPMSAttestation>> hpmsAttestationCallback, List<String> contractIds) {
        Set<HPMSAttestation> retAttests = new HashSet<>();
        for (String contractId : contractIds) {
            if (!attests.containsKey(contractId)) {
                throw new IllegalArgumentException(String.format("%s not found in test data.", contractId));
            }
            retAttests.add(attests.get(contractId));
        }
        hpmsAttestationCallback.accept(retAttests);
    }
}
