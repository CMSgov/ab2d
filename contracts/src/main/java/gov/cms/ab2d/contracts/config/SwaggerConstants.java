package gov.cms.ab2d.contracts.config;

public final class SwaggerConstants {
    public static final String MAIN = "HPMS is the source of truth for PDP attestation. " +
            "To ensure that the PDP has access to the claims data, periodically we will retrieve data from their API and update our data. " +
            "We have the ability to overwrite data and specify if we want it to automatically update or not";

    // Bulk Export API
    public static final String CONTRACTS_MAIN = "Request data about a contract and overwrite data or automatic updates.";

    public static final String ALL_CONTRACTS = "Get all Contracts or a single Contract base on contractId";

    public static final String UPDATE_CONTRACTS = "Update Contract information";

    public static final String GET_CONTRACT_BY_NUMBER = "Get a Contract with Contract Number";
}
