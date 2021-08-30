package gov.cms.ab2d.fhir;

import lombok.Data;

import static gov.cms.ab2d.fhir.IdentifierUtils.BENEFICIARY_ID;

@Data
public class PatientIdentifier {
    public static final String CURRENT_MBI = "current";
    public static final String HISTORIC_MBI = "historic";
    public static final String MBI_ID = "http://hl7.org/fhir/sid/us-mbi";
    public static final String MBI_ID_R4 = "http://terminology.hl7.org/CodeSystem/v2-0203";
    public static final String MBI_HASH_URL = "https://bluebutton.cms.gov/resources/identifier/mbi-hash";

    public enum Type {
        MBI(MBI_ID),
        MBI_R4(MBI_ID_R4),
        BENE_ID(BENEFICIARY_ID),
        MBI_HASH(MBI_HASH_URL);

        private final String system;

        Type(String system) {
            this.system = system;
        }

        public String getSystem() {
            return system;
        }

        public static Type fromSystem(String system) {
            Type[] values = Type.values();
            for (Type t : values) {
                if (t.system.equalsIgnoreCase(system)) {
                    return t;
                }
            }
            return null;
        }
    }

    public enum Currency {
        UNKNOWN,
        HISTORIC,
        CURRENT
    }

    private String value;
    private Type type;
    private Currency currency;

    public Long getValueAsLong() {
        if (value == null) {
            return null;
        }

        return Long.parseLong(value);
    }
}
