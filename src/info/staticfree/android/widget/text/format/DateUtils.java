/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.staticfree.android.widget.text.format;

import java.util.Formatter;

import android.content.Context;
import android.content.res.Resources;
import android.text.format.Time;

//@formatter:off

/**
 * This class contains various date-related utilities for creating text for things like
 * elapsed time and date ranges, strings for days of the week and months, and AM/PM text etc.
 */
public class DateUtils
{

    public static final long SECOND_IN_MILLIS = 1000;
    public static final long MINUTE_IN_MILLIS = SECOND_IN_MILLIS * 60;
    public static final long HOUR_IN_MILLIS = MINUTE_IN_MILLIS * 60;
    public static final long DAY_IN_MILLIS = HOUR_IN_MILLIS * 24;
    public static final long WEEK_IN_MILLIS = DAY_IN_MILLIS * 7;
    /**
     * This constant is actually the length of 364 days, not of a year!
     */
    public static final long YEAR_IN_MILLIS = WEEK_IN_MILLIS * 52;

    // The following FORMAT_* symbols are used for specifying the format of
    // dates and times in the formatDateRange method.
    public static final int FORMAT_SHOW_TIME = 0x00001;
    public static final int FORMAT_SHOW_WEEKDAY = 0x00002;
    public static final int FORMAT_SHOW_YEAR = 0x00004;
    public static final int FORMAT_NO_YEAR = 0x00008;
    public static final int FORMAT_SHOW_DATE = 0x00010;
    public static final int FORMAT_NO_MONTH_DAY = 0x00020;
    public static final int FORMAT_12HOUR = 0x00040;
    public static final int FORMAT_24HOUR = 0x00080;
    public static final int FORMAT_CAP_AMPM = 0x00100;
    public static final int FORMAT_NO_NOON = 0x00200;
    public static final int FORMAT_CAP_NOON = 0x00400;
    public static final int FORMAT_NO_MIDNIGHT = 0x00800;
    public static final int FORMAT_CAP_MIDNIGHT = 0x01000;
    /**
     * @deprecated Use
     * {@link #formatDateRange(Context, Formatter, long, long, int, String) formatDateRange}
     * and pass in {@link Time#TIMEZONE_UTC Time.TIMEZONE_UTC} for the timeZone instead.
     */
    @Deprecated
    public static final int FORMAT_UTC = 0x02000;
    public static final int FORMAT_ABBREV_TIME = 0x04000;
    public static final int FORMAT_ABBREV_WEEKDAY = 0x08000;
    public static final int FORMAT_ABBREV_MONTH = 0x10000;
    public static final int FORMAT_NUMERIC_DATE = 0x20000;
    public static final int FORMAT_ABBREV_RELATIVE = 0x40000;
    public static final int FORMAT_ABBREV_ALL = 0x80000;
    public static final int FORMAT_CAP_NOON_MIDNIGHT = (FORMAT_CAP_NOON | FORMAT_CAP_MIDNIGHT);
    public static final int FORMAT_NO_NOON_MIDNIGHT = (FORMAT_NO_NOON | FORMAT_NO_MIDNIGHT);

    private static int sRes_preposition_for_date;
    private static int sRes_preposition_for_time;
    private static int sRes_relative_time;
    private static int sRes_date_time;

    /**
     * Return string describing the elapsed time since startTime formatted like
     * "[relative time/date], [time]".
     * <p>
     * Example output strings for the US date format.
     * <ul>
     * <li>3 mins ago, 10:15 AM</li>
     * <li>yesterday, 12:20 PM</li>
     * <li>Dec 12, 4:12 AM</li>
     * <li>11/14/2007, 8:20 AM</li>
     * </ul>
     *
     * @param time some time in the past.
     * @param minResolution the minimum elapsed time (in milliseconds) to report
     *            when showing relative times. For example, a time 3 seconds in
     *            the past will be reported as "0 minutes ago" if this is set to
     *            {@link #MINUTE_IN_MILLIS}.
     * @param transitionResolution the elapsed time (in milliseconds) at which
     *            to stop reporting relative measurements. Elapsed times greater
     *            than this resolution will default to normal date formatting.
     *            For example, will transition from "6 days ago" to "Dec 12"
     *            when using {@link #WEEK_IN_MILLIS}.
     */
    // keep
    public static CharSequence getRelativeDateTimeString(Context c, long time, long minResolution,
            long transitionResolution, int flags) {
        final Resources r = Resources.getSystem();

        final long now = System.currentTimeMillis();
        final long duration = Math.abs(now - time);

        // getRelativeTimeSpanString() doesn't correctly format relative dates
        // above a week or exact dates below a day, so clamp
        // transitionResolution as needed.
        if (transitionResolution > WEEK_IN_MILLIS) {
            transitionResolution = WEEK_IN_MILLIS;
        } else if (transitionResolution < DAY_IN_MILLIS) {
            transitionResolution = DAY_IN_MILLIS;
        }

        initResources(c);

        final CharSequence timeClause = android.text.format.DateUtils.formatDateRange(c, time,
                time, FORMAT_SHOW_TIME);

        String result;
        if (duration < transitionResolution) {
            final CharSequence relativeClause = android.text.format.DateUtils.getRelativeTimeSpanString(time, now, minResolution, flags);
            result = r.getString(sRes_relative_time, relativeClause, timeClause);
        } else {
            final CharSequence dateClause = getRelativeTimeSpanString(c, time, false);
            result = r.getString(sRes_date_time, dateClause, timeClause);
        }

        return result;
    }

    /**
     * @return a relative time string to display the time expressed by millis.  Times
     * are counted starting at midnight, which means that assuming that the current
     * time is March 31st, 0:30:
     * <ul>
     *   <li>"millis=0:10 today" will be displayed as "0:10"</li>
     *   <li>"millis=11:30pm the day before" will be displayed as "Mar 30"</li>
     * </ul>
     * If the given millis is in a different year, then the full date is
     * returned in numeric format (e.g., "10/12/2008").
     *
     * @param withPreposition If true, the string returned will include the correct
     * preposition ("at 9:20am", "on 10/12/2008" or "on May 29").
     */
    // buggy method
    public static CharSequence getRelativeTimeSpanString(Context c, long millis,
            boolean withPreposition) {

        String result;
        final long now = System.currentTimeMillis();
        final long span = Math.abs(now - millis);

        synchronized (DateUtils.class) {
            if (sNowTime == null) {
                sNowTime = new Time();
            }

            if (sThenTime == null) {
                sThenTime = new Time();
            }

            sNowTime.set(now);
            sThenTime.set(millis);

            initResources(c);

            int prepositionId;
            if (span < DAY_IN_MILLIS && sNowTime.weekDay == sThenTime.weekDay) {
                // Same day
                final int flags = FORMAT_SHOW_TIME;
                result = android.text.format.DateUtils.formatDateRange(c, millis, millis, flags);
                prepositionId = sRes_preposition_for_time;
            } else if (sNowTime.year != sThenTime.year) {
                // Different years
                final int flags = FORMAT_SHOW_DATE | FORMAT_SHOW_YEAR | FORMAT_NUMERIC_DATE;
                result = android.text.format.DateUtils.formatDateRange(c, millis, millis, flags);

                // This is a date (like "10/31/2008" so use the date preposition)
                prepositionId = sRes_preposition_for_date;
            } else {
                // Default
                final int flags = FORMAT_SHOW_DATE | FORMAT_ABBREV_MONTH;
                result = android.text.format.DateUtils.formatDateRange(c, millis, millis, flags);
                prepositionId = sRes_preposition_for_date;
            }
            if (withPreposition) {
                final Resources res = Resources.getSystem();
                result = res.getString(prepositionId, result);
            }
        }
        return result;
    }

    private static void initResources(Context c) {
        final int thisContextHash = c.hashCode();
        if (sContextHash == 0 || thisContextHash != sContextHash){

            final Resources r = Resources.getSystem();
            sRes_preposition_for_date = r.getIdentifier("preposition_for_date", "string", "android");
            sRes_preposition_for_time = r.getIdentifier("preposition_for_time", "string", "android");
            sRes_relative_time = r.getIdentifier("relative_time", "string", "android");
            sRes_date_time = r.getIdentifier("date_time", "string", "android");

            sContextHash = thisContextHash;
        }

    }

    private static int sContextHash = 0;

    private static Time sNowTime;
    private static Time sThenTime;
}
