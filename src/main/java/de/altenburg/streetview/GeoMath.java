package de.altenburg.streetview;

/**
 * Reine Mathe-Hilfen ohne Minecraft-Abhaengigkeit.
 * Minecraft: +X = Osten, +Z = Sueden (Norden ist also -Z).
 */
public final class GeoMath {
    public static final double EARTH_RADIUS_M = 6_371_008.8;

    /** Ungefaehres Zentrum von Altenburg (Thueringen) als Standard-Ursprung. */
    public static final double ALTENBURG_LAT = 50.9847;
    public static final double ALTENBURG_LON = 12.4372;

    private GeoMath() {}

    /** Entfernung zweier GPS-Punkte in Metern (Haversine). */
    public static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double p1 = Math.toRadians(lat1);
        double p2 = Math.toRadians(lat2);
        double dp = p2 - p1;
        double dl = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dp / 2) * Math.sin(dp / 2)
                + Math.cos(p1) * Math.cos(p2) * Math.sin(dl / 2) * Math.sin(dl / 2);
        return 2 * EARTH_RADIUS_M * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }

    /** Block-Koordinate -> [lat, lon]. */
    public static double[] blockToLatLon(double x, double z, double originLat, double originLon, double metersPerBlock) {
        double metersPerDegLat = Math.PI * EARTH_RADIUS_M / 180.0;
        double lat = originLat - (z * metersPerBlock) / metersPerDegLat;
        double metersPerDegLon = metersPerDegLat * Math.cos(Math.toRadians(originLat));
        double lon = originLon + (x * metersPerBlock) / metersPerDegLon;
        return new double[] {lat, lon};
    }

    /** [lat, lon] -> Block-Koordinate [x, z]. */
    public static double[] latLonToBlock(double lat, double lon, double originLat, double originLon, double metersPerBlock) {
        double metersPerDegLat = Math.PI * EARTH_RADIUS_M / 180.0;
        double metersPerDegLon = metersPerDegLat * Math.cos(Math.toRadians(originLat));
        double z = -((lat - originLat) * metersPerDegLat) / metersPerBlock;
        double x = ((lon - originLon) * metersPerDegLon) / metersPerBlock;
        return new double[] {x, z};
    }
}
