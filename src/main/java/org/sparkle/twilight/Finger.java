package org.sparkle.twilight;

public class Finger {
    private final int id;
    private final String address;
    private long time;

    public Finger(int id, String address) {
        this.id = id;
        this.address = address;
        updateTimeStamp();
    }

    public void updateTimeStamp() {
        time = System.currentTimeMillis();
    }

    public int getId() {
        return id;
    }

    public String getAddress() {
        return address;
    }

    public long getTime() {
        return time;
    }

    @Override
    public String toString() {
        return "" + address;
    }
}