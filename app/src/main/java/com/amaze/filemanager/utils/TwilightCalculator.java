package com.amaze.filemanager.utils;

import android.text.format.DateUtils;

public class TwilightCalculator {

    public static final int DAY = 0;
    public static final int NIGHT = 1;

    private static final float DEGREES_TO_RADIANS = (float) (Math.PI / 180.0f);

    // element for calculating solar transit.
    private static final float J0 = 0.0009f;

    // correction for civil twilight
    private static final float ALTITUDE_CORRECTION_CIVIL_TWILIGHT = -0.104719755f;

    // coefficients for calculating Equation of Center.
    private static final float C1 = 0.0334196f;
    private static final float C2 = 0.000349066f;
    private static final float C3 = 0.000005236f;

    private static final float OBLIQUITY = 0.40927971f;

    // Java time on Jan 1, 2000 12:00 UTC.
    private static final long UTC_2000 = 946728000000L;

    /**
     * calculates the civil twilight bases on time and geo-coordinates.
     *
     * @param time time in milliseconds.
     * @param latitude latitude in degrees.
     * @param longitude latitude in degrees.
     */
    public int calculateTwilight(long time, double latitude, double longitude) {
        final float daysSince2000 = (float) (time - UTC_2000) / DateUtils.DAY_IN_MILLIS;

        // mean anomaly
        final float meanAnomaly = 6.240059968f + daysSince2000 * 0.01720197f;

        // true anomaly
        final float trueAnomaly = (float) (meanAnomaly + C1 * Math.sin(meanAnomaly) + C2
                * Math.sin(2 * meanAnomaly) + C3 * Math.sin(3 * meanAnomaly));

        // ecliptic longitude
        final float solarLng = trueAnomaly + 1.796593063f + (float) Math.PI;

        // solar transit in days since 2000
        final double arcLongitude = -longitude / 360;
        float n = Math.round(daysSince2000 - J0 - arcLongitude);
        double solarTransitJ2000 = n + J0 + arcLongitude + 0.0053f * Math.sin(meanAnomaly)
                + -0.0069f * Math.sin(2 * solarLng);

        // declination of sun
        double solarDec = Math.asin(Math.sin(solarLng) * Math.sin(OBLIQUITY));

        final double latRad = latitude * DEGREES_TO_RADIANS;

        double cosHourAngle = (Math.sin(ALTITUDE_CORRECTION_CIVIL_TWILIGHT) - Math.sin(latRad)
                * Math.sin(solarDec)) / (Math.cos(latRad) * Math.cos(solarDec));
        // The day or night never ends for the given date and location, if this value is out of
        // range.

        int mState;
        if (cosHourAngle >= 1) {
            mState = NIGHT;
        } else if (cosHourAngle <= -1) {
            mState = DAY;
        } else {
            float hourAngle = (float) (Math.acos(cosHourAngle) / (2 * Math.PI));

            long mSunset = Math.round((solarTransitJ2000 + hourAngle) * DateUtils.DAY_IN_MILLIS) + UTC_2000;
            long mSunrise = Math.round((solarTransitJ2000 - hourAngle) * DateUtils.DAY_IN_MILLIS) + UTC_2000;

            if (mSunrise < time && mSunset > time) {
                mState = DAY;
            } else {
                mState = NIGHT;
            }
        }
        return mState;
    }
}
