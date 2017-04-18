package com.getui.rewrite.test;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by fox on 18/04/2017.
 */
public class LogUtil {
    static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    public static void log(String message) {
        _log(message);
    }

    public static void log(String tag, String message) {
        _log(tag + ":" + message);
    }

    public static void debug(String message) {
        _log(message);
    }

    private static void _log(String msg) {
        System.out.println(format.format(new Date()) + "|" + msg);
    }

}
