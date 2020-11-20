package gov.cms.ab2d.common.util;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Coverage {


    private final Long id;

    private final int periodId;

    private final long searchEventId;

    private final String beneficiaryId;

    private final String currentMbi;

    private final List<String> historicMbis;

    public Coverage(Long id, int periodId, long searchEventId, String beneficiaryId, String currentMbi, String historicMbis) {
        this.id = id;
        this.periodId = periodId;
        this.searchEventId = searchEventId;
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