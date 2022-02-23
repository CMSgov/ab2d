package gov.cms.ab2d.common.model;

public class TooFrequentInvocations extends RuntimeException {

    public TooFrequentInvocations(String message) {
        super(message);
    }
}
