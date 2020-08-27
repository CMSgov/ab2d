package gov.cms.ab2d.hpms.hmsapi;

import lombok.Data;

import java.time.format.DateTimeFormatter;

@Data
public class HPMSAttestation {

    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    private String contractId;
    private boolean attested;
    private String attestationDate;
}
