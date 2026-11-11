package br.com.valence.api.utils;

public final class GeoUtils {

    private static final double RAIO_TERRA_KM = 6371.0;

    private GeoUtils() {
    }

    public static double distanciaKm(double latitude1, double longitude1, double latitude2, double longitude2) {
        double deltaLatitude = Math.toRadians(latitude2 - latitude1);
        double deltaLongitude = Math.toRadians(longitude2 - longitude1);
        double a = Math.sin(deltaLatitude / 2) * Math.sin(deltaLatitude / 2)
                + Math.cos(Math.toRadians(latitude1)) * Math.cos(Math.toRadians(latitude2))
                * Math.sin(deltaLongitude / 2) * Math.sin(deltaLongitude / 2);
        return RAIO_TERRA_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public static double grausDeLatitude(double km) {
        return km / 111.0;
    }

    public static double grausDeLongitude(double km, double latitude) {
        double cosseno = Math.max(Math.cos(Math.toRadians(latitude)), 0.01);
        return km / (111.0 * cosseno);
    }
}
