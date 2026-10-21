package gov.cms.ab2d.importer;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public record BackfillRequest(YearMonth month, List<String> contracts) {

    private static final Pattern CONTRACT = Pattern.compile("[A-Z]\\d{4}");
    private static final DateTimeFormatter COMPACT = DateTimeFormatter.ofPattern("yyyyMM");

    public BackfillRequest {
        contracts = List.copyOf(contracts);
    }

    public static BackfillRequest parse(String month, String contracts, YearMonth currentMonth) {
        YearMonth parsedMonth;
        try {
            parsedMonth = YearMonth.parse(month.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("BACKFILL_MONTH must be yyyy-MM, got '" + month + "'", e);
        }

        YearMonth cutoff = currentMonth.minusMonths(2);
        if (!parsedMonth.isBefore(cutoff)) {
            throw new IllegalArgumentException("BACKFILL_MONTH " + parsedMonth + " is not below the archive cutoff "
                    + cutoff + "; it is still in the rolling import window and the daily import owns it");
        }

        List<String> parsedContracts = contracts == null ? List.of() : Arrays.stream(contracts.split(","))
                .map(c -> c.trim().toUpperCase(Locale.ROOT))
                .filter(c -> !c.isEmpty())
                .distinct()
                .toList();
        if (parsedContracts.isEmpty()) {
            throw new IllegalArgumentException("BACKFILL_CONTRACTS must list at least one contract");
        }
        for (String contract : parsedContracts) {
            if (!CONTRACT.matcher(contract).matches()) {
                throw new IllegalArgumentException("Invalid contract number in BACKFILL_CONTRACTS: '" + contract + "'");
            }
        }

        return new BackfillRequest(parsedMonth, parsedContracts);
    }

    public String s3Key() {
        return "coverage_v3_backfill_" + month.format(COMPACT) + ".csv";
    }
}
