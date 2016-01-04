package de.velcommuta.denul.data;

/**
 * Placeholder data type to replace the Android Location object
 */
public class Location {
    private double latitude;
    private double longitude;
    private double time;

    public Location() {};

    public void setLatitude(double lat) {
        latitude = lat;
    }

    public void setLongitude(double longi) {
        longitude = longi;
    }

    public void setTime(double timestamp) {
        time = timestamp;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getTime() {
        return time;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (!(o instanceof Location)) return false;
        Location cmp = (Location) o;
        return (getLatitude() == cmp.getLatitude() && getLongitude() == cmp.getLongitude() && getTime() == cmp.getTime());
    }
}
