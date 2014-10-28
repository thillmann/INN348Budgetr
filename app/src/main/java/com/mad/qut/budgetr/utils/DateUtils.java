package com.mad.qut.budgetr.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;

public class DateUtils {

    public static Calendar getClearCalendar() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.clear(Calendar.MINUTE);
        cal.clear(Calendar.SECOND);
        cal.clear(Calendar.MILLISECOND);
        return cal;
    }

    public static String getFormattedDate(long timestamp, String format) {
        Date d = new Date(timestamp);
        java.text.DateFormat df = new SimpleDateFormat(format);
        return df.format(d);
    }

    public static String getFormattedDate(Date d, String format) {
        java.text.DateFormat df = new SimpleDateFormat(format);
        return df.format(d);
    }

    public static String getFormattedDateRange(long start, long end, String format, String delimiter) {
        Date startDate = new Date(start);
        Date endDate = new Date(end);
        java.text.DateFormat df = new SimpleDateFormat(format);
        return df.format(startDate) + " " + delimiter + " " + df.format(endDate);
    }

    public static long getTimeStampFromString(String date, String format) {
        try {
            Date d = new SimpleDateFormat(format, Locale.ENGLISH).parse(date);
            return d.getTime();
        } catch (Exception e) {
            return -1;
        }
    }

    public static long getCurrentTimeStamp() {
        Calendar c = getClearCalendar();
        return c.getTimeInMillis() / 1000;
    }

}
