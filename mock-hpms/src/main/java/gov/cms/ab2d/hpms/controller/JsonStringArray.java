package gov.cms.ab2d.hpms.controller;

import java.util.List;

public class JsonStringArray {

    private final List<String> values;

    public JsonStringArray(List<String> values) {
        this.values = values;
    }

    public List<String> getValues() {
        return values;
    }
}
