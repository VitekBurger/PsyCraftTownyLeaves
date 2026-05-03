package mao.psyCraftTowny.service;

final class CapturePoint {
    private final int id;
    private final String world;
    private final int pointX;
    private final int pointY;
    private final int pointZ;
    private double progress;
    private int ownerTeam;
    private int markerY;
    private int markerX;
    private int markerZ;
    private int groundY;

    CapturePoint(int id, String world, int pointX, int pointY, int pointZ, double progress, int ownerTeam) {
        this.id = id;
        this.world = world;
        this.pointX = pointX;
        this.pointY = pointY;
        this.pointZ = pointZ;
        this.progress = progress;
        this.ownerTeam = ownerTeam;
        this.markerY = -1;
        this.markerX = -1;
        this.markerZ = -1;
        this.groundY = -1;
    }

    int id() {
        return id;
    }

    String world() {
        return world;
    }

    int pointX() {
        return pointX;
    }

    int pointY() {
        return pointY;
    }

    int pointZ() {
        return pointZ;
    }

    double progress() {
        return progress;
    }

    void setProgress(double progress) {
        this.progress = progress;
    }

    int ownerTeam() {
        return ownerTeam;
    }

    void setOwnerTeam(int ownerTeam) {
        this.ownerTeam = ownerTeam;
    }

    int markerY() {
        return markerY;
    }

    void setMarkerY(int markerY) {
        this.markerY = markerY;
    }

    int markerX() {
        return markerX;
    }

    void setMarkerX(int markerX) {
        this.markerX = markerX;
    }

    int markerZ() {
        return markerZ;
    }

    void setMarkerZ(int markerZ) {
        this.markerZ = markerZ;
    }

    int groundY() {
        return groundY;
    }

    void setGroundY(int groundY) {
        this.groundY = groundY;
    }
}
