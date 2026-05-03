package mao.psyCraftTowny.model;

public class CapturePoint {
    private final int id;
    private final String displayName;
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

    public CapturePoint(int id, String displayName, String world, int pointX, int pointY, int pointZ, double progress, int ownerTeam) {
        this.id = id;
        this.displayName = displayName;
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

    public int id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public String world() {
        return world;
    }

    public int pointX() {
        return pointX;
    }

    public int pointY() {
        return pointY;
    }

    public int pointZ() {
        return pointZ;
    }

    public double progress() {
        return progress;
    }

    public void setProgress(double progress) {
        this.progress = progress;
    }

    public int ownerTeam() {
        return ownerTeam;
    }

    public void setOwnerTeam(int ownerTeam) {
        this.ownerTeam = ownerTeam;
    }

    public int markerY() {
        return markerY;
    }

    public void setMarkerY(int markerY) {
        this.markerY = markerY;
    }

    public int markerX() {
        return markerX;
    }

    public void setMarkerX(int markerX) {
        this.markerX = markerX;
    }

    public int markerZ() {
        return markerZ;
    }

    public void setMarkerZ(int markerZ) {
        this.markerZ = markerZ;
    }

    public int groundY() {
        return groundY;
    }

    public void setGroundY(int groundY) {
        this.groundY = groundY;
    }
}
