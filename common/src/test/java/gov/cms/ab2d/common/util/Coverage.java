package gov.cms.ab2d.common.util;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Coverage {

    private final int coveragePeriod;

    private final long searchEventId;

    private final String contract;

    private final int year;

    private final int month;

    private final Long beneficiaryId;

    private final String currentMbi;

    private final List<String> historicMbis;

    public Coverage(int coveragePeriod, long searchEventId, String contract, int year, int month,
                    Long beneficiaryId, String currentMbi, String historicMbis) {
        this.coveragePeriod = coveragePeriod;
        this.searchEventId = searchEventId;
        this.contract = contract;
        this.year = year;
        this.month = month;
        this.beneficiaryId = beneficiaryId;
        this.currentMbi = currentMbi;

        if (historicMbis != null) {
            String[] mbis = historicMbis.split(",");
            this.historicMbis = List.of(mbis);
        } else {
            this.historicMbis = new ArrayList<>();
        }
    }
}