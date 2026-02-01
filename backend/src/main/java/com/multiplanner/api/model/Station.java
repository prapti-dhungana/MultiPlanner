package com.multiplanner.api.model;

public class Station {

    private String code;
    private String name;
    private String town;

    public Station(String code, String name, String town) {
        this.code = code;
        this.name = name;
        this.town = town;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getTown() {
        return town;
    }
}

