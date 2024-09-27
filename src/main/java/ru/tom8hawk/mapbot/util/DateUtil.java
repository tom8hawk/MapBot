package ru.tom8hawk.mapbot.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateUtil {

    private static final SimpleDateFormat dateFormat =
            new SimpleDateFormat("d MMM", new Locale("ru"));

    public static String formatDate(Date date) {
        return dateFormat.format(date);
    }
}