package de.altenburg.streetview;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeoMathTest {
    @Test
    void oneDegreeLatitudeIsAbout111km() {
        assertEquals(111_195, GeoMath.distanceMeters(50, 12, 51, 12), 100);
    }

    @Test
    void samePointIsZero() {
        assertEquals(0.0, GeoMath.distanceMeters(50.98, 12.43, 50.98, 12.43), 1e-9);
    }

    @Test
    void blockRoundTrip() {
        double[] ll = GeoMath.blockToLatLon(250, -400, GeoMath.ALTENBURG_LAT, GeoMath.ALTENBURG_LON, 1.0);
        double[] b = GeoMath.latLonToBlock(ll[0], ll[1], GeoMath.ALTENBURG_LAT, GeoMath.ALTENBURG_LON, 1.0);
        assertEquals(250, b[0], 1e-6);
        assertEquals(-400, b[1], 1e-6);
    }

    @Test
    void blockDistanceMatchesMeters() {
        double[] a = GeoMath.blockToLatLon(0, 0, GeoMath.ALTENBURG_LAT, GeoMath.ALTENBURG_LON, 1.0);
        double[] b = GeoMath.blockToLatLon(300, 400, GeoMath.ALTENBURG_LAT, GeoMath.ALTENBURG_LON, 1.0);
        assertEquals(500, GeoMath.distanceMeters(a[0], a[1], b[0], b[1]), 1.0);
    }
}
