package com.example.next_contest.wear.model;

public class SavedPlace {
    public final double latitude;
    public final double longitude;
    public final String address;

    public SavedPlace(double latitude, double longitude, String address) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
    }
}
