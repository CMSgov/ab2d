package gov.cms.ab2d.lambdalibs.lib;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;

public class ParameterStoreUtil {

    private final String role;
    private final String dbUser;
    private final String dbPassword;

    public ParameterStoreUtil(String role, String dbUser, String dbPassword) {
        this.role = role;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
    }

    public static SsmClient getClient() {
        return  SsmClient.builder()
                .region(Region.US_EAST_1)
                .build();
    }

    public static ParameterStoreUtil getParameterStore(String roleParam, String dbUserParam, String dbPasswordParam) {
        var ssmClient = ParameterStoreUtil.getClient();

        var role = getValueFromParameterStore(roleParam, ssmClient);
        var dbUser = getValueFromParameterStore(dbUserParam, ssmClient);
        var dbPassword = getValueFromParameterStore(dbPasswordParam, ssmClient);

        ssmClient.close();
        return new ParameterStoreUtil(role, dbUser, dbPassword);
    }

    private static String getValueFromParameterStore(String key, SsmClient ssmClient) {
        var parameterRequest = GetParameterRequest.builder()
                .name(key)
                .withDecryption(true)
                .build();

        var parameterResponse = ssmClient.getParameter(parameterRequest);
        return parameterResponse.parameter().value();
    }

    public String getDbUser() {
        return dbUser;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public String getRole() {
        return role;
    }
}
