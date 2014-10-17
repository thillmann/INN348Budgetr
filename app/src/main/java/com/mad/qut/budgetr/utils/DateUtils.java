package com.mad.qut.budgetr.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils {

    public static String getFormattedDate(long timestamp, String format) {
        Date d = new Date(timestamp*1000);
        java.text.DateFormat df = new SimpleDateFormat(format);
        return df.format(d);
    }

}
