package com.multiplanner.api.model;

/**
 * Station model used for routing and autocomplete.
 *
 * Represents a London rail station identified by a (NaPTAN) code and
 * a more readable name
 */

public class Station {

    private String code; // NaPTAN stop/station code
    private String name;

    public Station(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}

