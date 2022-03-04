package gov.cms.ab2d.worker.model;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;


public interface ContractWorker {
    DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd H:m:s Z");

    boolean hasAttestation();

    ZonedDateTime getESTAttestationTime();

    boolean hasDateIssue();

    boolean equals(Object o);

    int hashCode();

    String toString();

    Long getId();

    String getContractNumber();

    String getContractName();

    ContractType getContractType();

    java.time.OffsetDateTime getAttestedOn();

    void setId(Long id);

    void setContractNumber(String contractNumber);

    void setContractName(String contractName);

    void setContractType(ContractType contractType);

    void setAttestedOn(java.time.OffsetDateTime attestedOn);

    enum UpdateMode { AUTOMATIC, NONE, MANUAL }

    enum ContractType { NORMAL, CLASSIC_TEST, SYNTHEA; }
}
