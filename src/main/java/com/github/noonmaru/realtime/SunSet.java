package com.github.noonmaru.realtime;


import java.util.Calendar;
import java.util.Date;

/**
 * @author 괴도군
 * 출처 https://rinear.tistory.com/entry/javaandroid
 */
public class SunSet
{

    private static final double PI = 3.141592;

    private static boolean isLeapYear(int year)
    {
        return ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0);
    }

    private static int getLastDay(int uiYear, int ucMonth)
    {
        switch (ucMonth)
        {
            case 2: // 2월
                if ((uiYear % 4) == 0)
                {        // 4로 나누어 떨어지는 해는 윤년임.
                    if (uiYear % 100 == 0)
                    {    // 그중에서 100으로 나누어 떨어지는 해는 평년임
                        if (uiYear % 400 == 0)
                            return 29; // 그중에서 400으로 나누어 떨어지는 해는 윤년임.
                        return 28; // 평년
                    }
                    return 29;    // 윤년
                }
                return 28;    // else 평년
            case 4:
            case 6:
            case 9:
            case 11: // 4, 6, 9, 11월
                return 30; // 30일
        }

        return 31; // 그외 31일
    }


    private static int calcJulianDay(int uiYear, int ucMonth, int ucDay)
    {
        int i;
        int iJulDay;
        iJulDay = 0;
        for (i = 1; i < ucMonth; i++)
        {
            iJulDay += getLastDay(uiYear, i);
        }
        iJulDay += ucDay;

        return iJulDay;
    }

    private static double calcGamma(int iJulDay)
    {
        return (2.0 * PI / 365.0) * (iJulDay - 1);
    }

    private static double calcGamma2(int iJulDay, int hour)
    {
        return (2.0 * PI / 365.0) * (iJulDay - 1 + (hour / 24.0));
    }

    // Return the equation of time value for the given date.
    private static double calcEqofTime(double gamma)
    {
        return (229.18 * (0.000075 + 0.001868 * Math.cos(gamma) - 0.032077 * Math.sin(gamma) - 0.014615 * Math.cos(2 * gamma) - 0.040849 * Math.sin(2 * gamma)));

    }

    // Return the solar declination angle (in radians) for the given date.
    private static double calcSolarDec(double gamma)
    {
        return (0.006918 - 0.399912 * Math.cos(gamma) + 0.070257 * Math.sin(gamma) - 0.006758 * Math.cos(2 * gamma) + 0.000907 * Math.sin(2 * gamma));
    }

    private static double degreeToRadian(double angleDeg)
    {
        return (PI * angleDeg / 180.0);
    }

    private static double radianToDegree(double angleRad)
    {
        return (180 * angleRad / PI);
    }

    private static double calcHourAngle(double latitude, double solarDec, int time)
    {
        double latRad = degreeToRadian(latitude);
        double hour_angle = Math.acos(Math.cos(degreeToRadian(90.833)) / (Math.cos(latRad) * Math.cos(solarDec)) - Math.tan(latRad) * Math.tan(solarDec));
        if (time == 1)
        {
            return hour_angle;
        }
        else if (time == 0)
        {
            return -hour_angle;
        }
        return 0;
    }

    private static double calcSunriseGMT(int iJulDay, double latitude, double longitude)
    {
        double gamma = calcGamma(iJulDay);
        double eqTime = calcEqofTime(gamma);
        double solarDec = calcSolarDec(gamma);
        double hourAngle = calcHourAngle(latitude, solarDec, 1);
        double delta = longitude - radianToDegree(hourAngle);
        double timeDiff = 4.0 * delta;
        double timeGMT = 720.0 + timeDiff - eqTime;
        double gamma_sunrise = calcGamma2(iJulDay, (int) (timeGMT / 60.0));
        eqTime = calcEqofTime(gamma_sunrise);
        solarDec = calcSolarDec(gamma_sunrise);
        hourAngle = calcHourAngle(latitude, solarDec, 1);
        delta = longitude - radianToDegree(hourAngle);
        timeDiff = 4.0 * delta;
        timeGMT = 720.0 + timeDiff - eqTime;

        return timeGMT;
    }

    private static double calcSunsetGMT(int iJulDay, double latitude, double longitude)
    {
        // First calculates sunrise and approx length of day
        double gamma = calcGamma(iJulDay + 1);
        double eqTime = calcEqofTime(gamma);
        double solarDec = calcSolarDec(gamma);
        double hourAngle = calcHourAngle(latitude, solarDec, 0);
        double delta = longitude - radianToDegree(hourAngle);
        double timeDiff = 4.0 * delta;
        double setTimeGMT = 720.0 + timeDiff - eqTime;
        // first pass used to include fractional day in gamma calc
        double gamma_sunset = calcGamma2(iJulDay, (int) (setTimeGMT / 60.0));
        eqTime = calcEqofTime(gamma_sunset);
        solarDec = calcSolarDec(gamma_sunset);
        hourAngle = calcHourAngle(latitude, solarDec, 0);
        delta = longitude - radianToDegree(hourAngle);
        timeDiff = 4.0 * delta;
        setTimeGMT = 720.0 + timeDiff - eqTime; // in minutes
        return setTimeGMT;
    }

    public static Date getSunriseTime(int year, int month, int day, double latitude, double longitude, int zone, int daySavings)
    {
        int julday = calcJulianDay(year, month, day);
        double timeLST = calcSunriseGMT(julday, latitude, longitude) - (60.0 * zone) + daySavings; // minutes

        double floatHour = timeLST / 60.0;
        int hour = (int) Math.floor(floatHour);
        double floatMinute = 60.0 * (floatHour - Math.floor(floatHour));
        int minute = (int) Math.floor(floatMinute);
        double floatSec = 60.0 * (floatMinute - Math.floor(floatMinute));
        int second = (int) Math.floor(floatSec);

        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, day, hour, minute, second);

        return calendar.getTime();
    }

    public static Date getSunsetTime(int year, int month, int day, double latitude, double longitude, int zone, int daySavings)
    {
        int julday = calcJulianDay(year, month, day);
        double timeLST = calcSunsetGMT(julday, latitude, longitude) - (60.0 * zone) + daySavings; // minutes

        double floatHour = timeLST / 60.0;
        int hour = (int) Math.floor(floatHour);
        double floatMinute = 60.0 * (floatHour - Math.floor(floatHour));
        int minute = (int) Math.floor(floatMinute);
        double floatSec = 60.0 * (floatMinute - Math.floor(floatMinute));
        int second = (int) Math.floor(floatSec);

        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, day, hour, minute, second);

        return calendar.getTime();
    }

    public static Date getSunsetTime(Calendar calendar, double latitude, double longitude, int timeZone)
    {
        return getSunsetTime(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH), latitude, longitude, timeZone, 0);
    }

    public static Date getSunriseTime(Calendar calendar, double latitude, double longitude, int timeZone)
    {
        return getSunriseTime(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH), latitude, longitude, timeZone, 0);
    }
}