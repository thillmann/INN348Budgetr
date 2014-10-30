package com.mad.qut.budgetr.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DateUtils {

    /**
     * Regular expressions for different date formats.
     */
    public static final Map<String, String> DATE_FORMAT_REGEXPS = new HashMap<String, String>() {{
        put("\\d{1,2}-\\d{1,2}-\\d{4}", "dd-MM-yyyy");
        put("\\d{1,2}/\\d{1,2}/\\d{4}", "dd/MM/yyyy");
        put("\\d{1,2}-\\d{1,2}-\\d{2}", "dd-MM-yy");
        put("\\d{1,2}/\\d{1,2}/\\d{2}", "dd/MM/yy");
        put("\\d{1,2}\\s[a-z]{3}\\s\\d{4}", "dd MMM yyyy");
        put("\\d{1,2}\\s[a-z]{3}\\s\\d{2}", "dd MMM yy");
        put("\\d{1,2}\\s[a-z]{4,}\\s\\d{4}", "dd MMMM yyyy");
        put("\\d{1,2}\\s[a-z]{4,}\\s\\d{24}", "dd MMMM yy");
    }};

    /**
     * Get a cleared (meaning h, min, s, ms are set to 0) calendar of the current date.
     *
     * @return Cleared calendar
     */
    public static Calendar getClearCalendar() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.clear(Calendar.MINUTE);
        cal.clear(Calendar.SECOND);
        cal.clear(Calendar.MILLISECOND);
        return cal;
    }

    /**
     * Format timestamp according to formatting string.
     *
     * @param timestamp Timestamp
     * @param format Date format
     * @return Formatted date string
     */
    public static String getFormattedDate(long timestamp, String format) {
        Date d = new Date(timestamp);
        java.text.DateFormat df = new SimpleDateFormat(format);
        return df.format(d);
    }

    /**
     * Format date according to formatting string.
     *
     * @param d Date
     * @param format Date format
     * @return Formatted date string
     */
    public static String getFormattedDate(Date d, String format) {
        java.text.DateFormat df = new SimpleDateFormat(format);
        return df.format(d);
    }

    /**
     * Format a time period () according to formatting string.
     *
     * @param start Timestamp of start date
     * @param end Timestamp of end date
     * @param format Date format
     * @param delimiter Delimiter string
     * @return Formatted time period string
     */
    public static String getFormattedDateRange(long start, long end, String format, String delimiter) {
        Date startDate = new Date(start);
        Date endDate = new Date(end);
        java.text.DateFormat df = new SimpleDateFormat(format);
        return df.format(startDate) + " " + delimiter + " " + df.format(endDate);
    }

    /**
     * Parse a string and return the corresponding timestamp.
     *
     * @param date String containing date.
     * @param format Date format
     * @return Timestamp
     */
    public static long getTimeStampFromString(String date, String format) {
        try {
            Date d = new SimpleDateFormat(format, Locale.ENGLISH).parse(date);
            return d.getTime();
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Returns current timestamp.
     *
     * @return Timestamp
     */
    public static long getCurrentTimeStamp() {
        Calendar c = getClearCalendar();
        return c.getTimeInMillis() / 1000;
    }

}
