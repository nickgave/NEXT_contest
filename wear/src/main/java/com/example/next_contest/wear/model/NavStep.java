package com.example.next_contest.wear.model;

public class NavStep {
    public final String instruction;
    public final double latitude;
    public final double longitude;
    public final int distanceMeters;

    public NavStep(String instruction, double latitude, double longitude, int distanceMeters) {
        this.instruction = instruction;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distanceMeters = distanceMeters;
    }
}
