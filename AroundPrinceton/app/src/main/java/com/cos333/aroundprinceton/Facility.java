package com.cos333.aroundprinceton;


// An object that stores all information about one facility or building item

public class Facility {
    private String type;
    private String name;
    private String building;
    private String details;
    private double lat;
    private double lon;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public String getDetails() {
        return details;
    }

    public String getDetailsShort() {
        if (details.length() < 80) return details;
        int i = 79;
        while (details.charAt(i) != ' ') i--;
        return details.substring(0, i) + "...";
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }
}
