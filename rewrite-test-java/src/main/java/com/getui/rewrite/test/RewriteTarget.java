package com.getui.rewrite.test;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by fox on 18/04/2017.
 */
public class RewriteTarget {
    static String TAG = "RewriteTarget";

    public void test() {
        //literal
//        LogUtil.log("i am the log message");

        // simple expression
//        LogUtil.log("hahahahhahaha" + new Date().toString() + "i am right expression");

        //format
        Calendar calendar = Calendar.getInstance();
//        SimpleDateFormat format = new SimpleDateFormat("hh:mm:ss");
//        LogUtil.log(TAG, String.format("now.... year:[%d], month:[%d], day:[%d], time:[%s]",
//                calendar.get(Calendar.YEAR)
//                , calendar.get(Calendar.MONTH)
//                , calendar.get(Calendar.DAY_OF_MONTH)
//                , format.format(calendar.getTime())
//        ));
        LogUtil.log(String.format(Locale.CHINA, "我来自%s省-%s市", "浙江", "杭州"));
        //complex expression
        String aaa = " i am aaa as variable ";
        String bbb = " i am bbb as variable ";
        LogUtil.log(TAG, " i am literal "
                + aaa + " i am another literal "
                + String.format(" year:[%d] month:[%d] "
                , calendar.get(Calendar.YEAR)
                , Calendar.MONTH)
                + bbb
                + 365
                + " i am last literal string"
        );
    }
}
