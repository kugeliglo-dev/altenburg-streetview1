package de.altenburg.streetview;

/** Ein Strassenfoto samt Position. */
public record StreetImage(String id, double lat, double lon, double compassAngle, boolean pano, String imageUrl) {

    public double distanceTo(double otherLat, double otherLon) {
        return GeoMath.distanceMeters(lat, lon, otherLat, otherLon);
    }
}
