package com.example.lending.loan.servicing.common.time;

import java.util.Calendar;
import java.util.Date;

/** Calendar arithmetic used by statements and collections (days past due, cycle lengths). */
public final class ServicingDates {

    private ServicingDates() {
    }

    public static int getIntervalDays(Date day1, Date day2) {
        if (day1 == null || day2 == null) {
            throw new IllegalArgumentException("Argument day1 or day2 must be not null.");
        }

        Date day1ToUse = truncateTime(day1);
        Date day2ToUse = truncateTime(day2);
        long intervalMilliSecond = getIntervalMilliSeconds(day1ToUse, day2ToUse);

        return (int) (intervalMilliSecond / (24 * 60 * 60 * 1000));
    }

    public static Date truncateTime(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    public static long getIntervalMilliSeconds(Date start, Date end) {
        return Math.abs(end.getTime() - start.getTime());
    }

    public static Date addDays(Date date, int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, days);
        return calendar.getTime();
    }
}
