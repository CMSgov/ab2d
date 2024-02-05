package gov.cms.ab2d.worker.processor.coverage.check;

public final class CenteneSupportCheck {

    private CenteneSupportCheck() {
    }

    public static boolean isCentene(String contractNum) {
        return (contractNum.equals("S4802") || contractNum.equals("Z1001"));
    }
}
