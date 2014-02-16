package info.staticfree.android.widget;

import info.staticfree.android.dearfutureself.R;

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;

public class TimelineEntry extends View {
    @SuppressWarnings("unused")
    private static final String TAG = TimelineEntry.class.getSimpleName();
    long mStartTime, mEndTime;
    long mMinTime = 0;

    private Context mContext;

    private static final Paint PAINT_AXIS = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint PAINT_TICK = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint PAINT_DISABLED = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static final Paint RED_OUTLINE = new Paint();

    // ms → s → m → h → d
    private static final int DEFAULT_SCALE = 1000 * 60 * 60 * 24;

    private static final int MIN_SCALE = 1000 * 60 * 5;
    private static final long MAX_SCALE = DateUtils.YEAR_IN_MILLIS * 10;

    private int mMajorTickSize = 40;
    private int mMinorTickSize = 10;

    private int TICK_LABEL_SIZE_LARGE = 20;
    private int TICK_LABEL_SIZE_SMALL = 6;

    private final int TICK_LABEL_SPACING = 3;

    private static final int TICK_LINE_SIZE = 4;

    /**
     * the point in the scale where odd-numbered labels are skipped.
     */
    private static final float FULL_LABELING_CUTOFF = 0.7f;

    private Drawable mMarker;

    private final DisplayMetrics mDisplayMetrics = new DisplayMetrics();

    private OnChangeListener mOnChangeListener;

    private int mWidth, mHeight;

    private int mPaddingLeft, mPaddingRight, mPaddingTop, mPaddingBottom;

    private final Rect mMarkerBounds = new Rect();
    private final Rect mDrawBounds = new Rect();

    private float mScaleX;
    private float mMultX;

    private GestureDetector mGestureDetector;
    private Scroller mScroller;

    private int mScrollX;
    private int mScrollY;

    private Interval[] mIntervals;
    private final HashSet<Long> mDrawnTicks = new HashSet<Long>();

    private static final int MSG_STOP_SCROLLING = 100;

    private static class ScrollHandler extends Handler {
        private final Scroller mScroller;

        public ScrollHandler(Scroller scroller) {
            mScroller = scroller;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_STOP_SCROLLING:
                    mScroller.abortAnimation();
                    break;
            }
        };
    };

    private ScrollHandler mHandler;

    private static final Paint PAINT_TICK_LABEL = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static final Paint PAINT_INTERVAL_LABEL = new Paint(Paint.ANTI_ALIAS_FLAG);
    /**
     * How far up from the bottom the label is
     */
    private static final float INTERVAL_LABEL_FROM_EDGE = 6;
    private static final float INTERVAL_LABEL_TEXT_SIZE = 12;

    static {
        PAINT_AXIS.setARGB(192, 0, 0, 0);
        PAINT_AXIS.setStyle(Style.STROKE);
        PAINT_AXIS.setStrokeWidth(2);

        PAINT_TICK.setARGB(128, 0, 0, 0);
        PAINT_TICK.setStyle(Style.STROKE);
        PAINT_TICK.setStrokeWidth(2);

        PAINT_TICK_LABEL.setARGB(255, 127, 127, 127);
        PAINT_TICK_LABEL.setTextAlign(Align.CENTER);

        PAINT_INTERVAL_LABEL.setARGB(255, 127, 127, 127);
        PAINT_INTERVAL_LABEL.setTextAlign(Align.CENTER);

        PAINT_DISABLED.setARGB(48, 0, 0, 0);
        PAINT_DISABLED.setStyle(Style.FILL);

        RED_OUTLINE.setColor(Color.RED);
        RED_OUTLINE.setStyle(Style.STROKE);
        RED_OUTLINE.setStrokeWidth(0);
    }

    public TimelineEntry(Context context) {
        super(context);
        init(context);
    }

    public TimelineEntry(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TimelineEntry(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        mIntervals =
                new Interval[] { new YearInterval(), new MonthInterval(context),
                        new WeekInterval(), new DayInterval(), new HourInterval(),
                        new MinuteInterval(15, true), new MinuteInterval() };

        mMarker = context.getResources().getDrawable(R.drawable.timeline_marker);

        mScroller = new Scroller(context, new DecelerateInterpolator(), true);
        mHandler = new ScrollHandler(mScroller);

        if (mStartTime == 0) {
            mEndTime = System.currentTimeMillis();

            mStartTime = mEndTime - DEFAULT_SCALE;
        }

        mCalendar = Calendar.getInstance();

        mGestureDetector = new GestureDetector(mContext, mGestureListener);

        final WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(mDisplayMetrics);

        mTickLineSize = mDisplayMetrics.scaledDensity * TICK_LINE_SIZE;

        PAINT_INTERVAL_LABEL.setTextSize(INTERVAL_LABEL_TEXT_SIZE * mDisplayMetrics.scaledDensity);

        mContext = context;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        final SavedState ss = new SavedState(superState, mStartTime, mEndTime);
        return ss;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        final SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        mStartTime = ss.mStartTime;
        mEndTime = ss.mEndTime;
    }

    private static class SavedState extends BaseSavedState {
        private final long mStartTime;
        private final long mEndTime;

        public SavedState(Parcelable superState, long startTime, long endTime) {
            super(superState);
            mStartTime = startTime;
            mEndTime = endTime;
        }

        public SavedState(Parcel in) {
            super(in);

            mStartTime = in.readLong();
            mEndTime = in.readLong();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeLong(mStartTime);
            dest.writeLong(mEndTime);
        }

        @SuppressWarnings("unused")
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Creator<TimelineEntry.SavedState>() {

                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }

                    @Override
                    public SavedState createFromParcel(Parcel source) {
                        return new SavedState(source);
                    }
                };
    }

    public void setOnChangeListener(OnChangeListener onChangeListener) {
        mOnChangeListener = onChangeListener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measuredWidth = View.MeasureSpec.getSize(widthMeasureSpec), measuredHeight =
                View.MeasureSpec.getSize(heightMeasureSpec);

        switch (View.MeasureSpec.getMode(widthMeasureSpec)) {
            case View.MeasureSpec.UNSPECIFIED:
                measuredWidth = getBackground().getIntrinsicWidth();
                break;
            case View.MeasureSpec.AT_MOST:

                break;
            case View.MeasureSpec.EXACTLY:
                // already set!
                break;
        }
        switch (View.MeasureSpec.getMode(heightMeasureSpec)) {
            case View.MeasureSpec.UNSPECIFIED:
                measuredHeight = getBackground().getIntrinsicHeight();
                break;
            case View.MeasureSpec.AT_MOST:
                measuredHeight = getBackground().getIntrinsicHeight();
                break;
            case View.MeasureSpec.EXACTLY:
                // already set!
                break;
        }
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mPaddingLeft = getPaddingLeft();
        mPaddingRight = getPaddingRight();
        mPaddingBottom = getPaddingBottom();
        mPaddingTop = getPaddingTop();
        mWidth = w - (mPaddingLeft) - mPaddingRight;
        mHeight = h - (mPaddingTop) - mPaddingBottom;

        // the below values are based on what happens to look good.
        mMajorTickSize = (int) (mHeight / 4.5);
        mMinorTickSize = mHeight / 13;

        TICK_LABEL_SIZE_LARGE = mHeight / 6;
        TICK_LABEL_SIZE_SMALL = mHeight / 50;
    }

    Calendar mCalendar;
    private float mTickLineSize;

    private final HashMap<Interval, CharSequence> mIntervalLabels =
            new HashMap<TimelineEntry.Interval, CharSequence>();

    @Override
    protected void onDraw(Canvas canvas) {
        final int w = mWidth;
        final int h = mHeight;
        final int hCenter = h / 2;

        final long timelineW = mEndTime - mStartTime;
        mScaleX = w / (float) timelineW;
        mMultX = (float) timelineW / w;

        canvas.save();

        // offset so drawing starts with the padding
        canvas.translate(mPaddingLeft, mPaddingTop);

        canvas.clipRect(0, 0, w, h);

        // main axis
        canvas.drawLine(0, hCenter, w, hCenter, PAINT_AXIS);

        // show the disabled region
        if (mMinTime > mStartTime) {
            canvas.drawRect(0, 0, (mMinTime - mStartTime) * mScaleX, h, PAINT_DISABLED);
        }

        final float maxTicks = w / 10.0f;

        final float minTicks = w / 100000.0f;

        mDrawnTicks.clear();

        CharSequence intervalLabel;

        boolean drawnIntervalLabel = false;
        for (final Interval interval : mIntervals) {
            final long fullCount = timelineW / interval.getApproxPeriod();

            if (fullCount < maxTicks && fullCount > minTicks) {
                final float scale = 1 - ((fullCount - minTicks) / maxTicks);
                // final float scaleLog = (float) (1 - Math.log(1 + (fullCount - 1) / maxTicks));
                PAINT_TICK.setAlpha((int) (scale * 255));
                if (scale > 0.8f) {
                    PAINT_TICK.setStrokeWidth((scale - 0.8f) * mTickLineSize + 1);
                } else {
                    PAINT_TICK.setStrokeWidth(1);
                }
                final float tickSize = ((mMajorTickSize - mMinorTickSize) * scale + mMinorTickSize);
                final float textSize =
                        Math.round(((TICK_LABEL_SIZE_LARGE - TICK_LABEL_SIZE_SMALL) * scale + TICK_LABEL_SIZE_SMALL));
                PAINT_TICK_LABEL.setTextSize(textSize);
                mCalendar.setTimeInMillis(mStartTime);
                interval.startTicking(mCalendar);
                int i = 0;
                for (long xMarker = interval.getNextTick(); xMarker >= mStartTime
                        && xMarker < mEndTime; xMarker = interval.getNextTick()) {
                    if (i > 1000) {
                        throw new RuntimeException("tried to draw too many ticks with interval "
                                + interval);
                    }
                    i++;

                    if (mDrawnTicks.contains(xMarker)) {
                        continue;
                    }

                    final float xPos = (xMarker - mStartTime) * mScaleX;
                    canvas.drawLine(xPos, hCenter + tickSize, xPos, hCenter - tickSize, PAINT_TICK);
                    final CharSequence label = interval.getTickLabel(scale);
                    if (label != null) {
                        canvas.drawText(label, 0, label.length(), xPos, hCenter - tickSize
                                - TICK_LABEL_SPACING * scale, PAINT_TICK_LABEL);
                    }

                    mDrawnTicks.add(xMarker);
                }
                if (!drawnIntervalLabel) {
                    intervalLabel = mIntervalLabels.get(interval);
                    if (intervalLabel == null) {
                        intervalLabel = interval.getLabel(mContext, scale);
                        mIntervalLabels.put(interval, intervalLabel);
                    }
                    canvas.drawText(intervalLabel, 0, intervalLabel.length(), w / 2, h
                            - INTERVAL_LABEL_FROM_EDGE * mDisplayMetrics.scaledDensity,
                            PAINT_INTERVAL_LABEL);
                    drawnIntervalLabel = true;
                }
            }
        }

        canvas.restore();

        canvas.getClipBounds(mDrawBounds);
        Gravity.apply(Gravity.CENTER_HORIZONTAL | Gravity.TOP, mMarker.getIntrinsicWidth(),
                mMarker.getIntrinsicHeight(), mDrawBounds, mMarkerBounds);
        mMarker.setBounds(mMarkerBounds);
        mMarker.draw(canvas);
    }

    private static interface Interval {
        /**
         * In order to determine when to show a given interval, provide an approximate amount of
         * time that this interval represents. For example, for a minute interval, return the number
         * of milliseconds in one minute.
         *
         * @return approximation of the interval, in ms
         */
        public long getApproxPeriod();

        /**
         * Initialize the interval with the given calendar. Further calls will be made to
         * {@link #getNextTick()} and {@link #getTickLabel(float)}.
         *
         * @param calendar
         */
        public void startTicking(Calendar calendar);

        /**
         * Gets the next marker. {@link #startTicking(Calendar)} must be called first.
         *
         * @return the next interval time, in Unix epoch time
         */
        public long getNextTick();

        /**
         * Gets a label for the given tick.
         *
         * @param scale
         *            The zoom factor of the graph. 1.0 means it's the main item; 0.0 means it's
         *            almost invisible.
         * @return the label of the same tick that was returned in {@link #getNextTick()}.
         */
        public CharSequence getTickLabel(float scale);

        public CharSequence getLabel(Context context, float scale);
    }

    private static class MinuteInterval implements Interval {

        private Calendar c;
        private final int mSubInterval;
        private boolean mSkipZero = false;

        public MinuteInterval() {
            mSubInterval = 1;
        }

        public MinuteInterval(int subInterval, boolean skipZero) {
            mSubInterval = subInterval;
            mSkipZero = skipZero;
        }

        public void startTicking(Calendar calendar) {
            this.c = (Calendar) calendar.clone();

            c.clear(Calendar.SECOND);
            c.clear(Calendar.MILLISECOND);
        }

        public long getNextTick() {
            final int min = c.get(Calendar.MINUTE);
            c.add(Calendar.MINUTE, mSubInterval - (min % mSubInterval));

            if (mSkipZero && c.get(Calendar.MINUTE) == 0) {
                return getNextTick();
            }
            return c.getTimeInMillis();
        }

        @Override
        public CharSequence getTickLabel(float scale) {
            final int min = c.get(Calendar.MINUTE);

            return mSubInterval > 1 || (scale > FULL_LABELING_CUTOFF || min % 2 == 0) ? String
                    .valueOf(min) : null;
        }

        @Override
        public long getApproxPeriod() {
            return DateUtils.MINUTE_IN_MILLIS * mSubInterval;
        }

        @Override
        public CharSequence getLabel(Context context, float scale) {
            return context.getText(R.string.minutes);
        }
    }

    private static class HourInterval implements Interval {

        private Calendar c;

        public void startTicking(Calendar calendar) {
            this.c = (Calendar) calendar.clone();

            c.clear(Calendar.MINUTE);
            c.clear(Calendar.SECOND);
            c.clear(Calendar.MILLISECOND);
        }

        public long getNextTick() {
            c.add(Calendar.HOUR_OF_DAY, 1);

            return c.getTimeInMillis();
        }

        @Override
        public CharSequence getTickLabel(float scale) {
            final int hour = c.get(Calendar.HOUR_OF_DAY);
            return scale > FULL_LABELING_CUTOFF || hour % 2 == 0 ? String.valueOf(hour) : null;
        }

        @Override
        public long getApproxPeriod() {
            return DateUtils.HOUR_IN_MILLIS;
        }

        @Override
        public CharSequence getLabel(Context context, float scale) {
            return context.getText(R.string.hours);
        }
    }

    private static class DayInterval implements Interval {

        private Calendar c;

        public void startTicking(Calendar calendar) {
            this.c = (Calendar) calendar.clone();

            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
        }

        public long getNextTick() {
            c.add(Calendar.DAY_OF_MONTH, 1);

            return c.getTimeInMillis();
        }

        @Override
        public long getApproxPeriod() {
            return DateUtils.DAY_IN_MILLIS;
        }

        @Override
        public CharSequence getTickLabel(float scale) {
            return String.valueOf(c.get(Calendar.DAY_OF_MONTH));
        }

        @Override
        public CharSequence getLabel(Context context, float scale) {
            return context.getText(R.string.days);
        }
    }

    private static class WeekInterval implements Interval {

        private Calendar c;

        public void startTicking(Calendar calendar) {
            this.c = (Calendar) calendar.clone();

            c.set(Calendar.DAY_OF_WEEK, c.getFirstDayOfWeek());
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
        }

        public long getNextTick() {
            c.add(Calendar.WEEK_OF_YEAR, 1);

            return c.getTimeInMillis();
        }

        @Override
        public long getApproxPeriod() {
            return DateUtils.WEEK_IN_MILLIS;
        }

        @Override
        public CharSequence getTickLabel(float scale) {
            return null;
        }

        @Override
        public CharSequence getLabel(Context context, float scale) {
            return context.getText(R.string.weeks);
        }
    }

    private static class MonthInterval implements Interval {

        private Calendar c;
        private final Context mContext;

        private static final float FULL_LABELING_CUTOFF_MONTH = Math.min(
                FULL_LABELING_CUTOFF + 0.1f, 1f);

        public MonthInterval(Context context) {
            mContext = context;
        }

        public void startTicking(Calendar calendar) {
            this.c = (Calendar) calendar.clone();

            c.set(Calendar.DAY_OF_MONTH, c.getMinimum(Calendar.DAY_OF_MONTH));
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
        }

        public long getNextTick() {
            c.add(Calendar.MONTH, 1);

            return c.getTimeInMillis();
        }

        @Override
        public long getApproxPeriod() {
            return DateUtils.DAY_IN_MILLIS * 30;
        }

        @Override
        public CharSequence getTickLabel(float scale) {

            if (scale > FULL_LABELING_CUTOFF_MONTH || c.get(Calendar.MONTH) % 2 == 0) {
                return DateUtils.formatDateTime(mContext, c.getTimeInMillis(),
                        DateUtils.FORMAT_NO_YEAR | DateUtils.FORMAT_SHOW_DATE
                                | DateUtils.FORMAT_NO_MONTH_DAY | DateUtils.FORMAT_NO_MIDNIGHT
                                | DateUtils.FORMAT_NO_NOON
                                | (scale < 0.95 ? DateUtils.FORMAT_ABBREV_MONTH : 0));
            }
            return null;
        }

        @Override
        public CharSequence getLabel(Context context, float scale) {
            return context.getText(R.string.months);
        }
    }

    private static class YearInterval implements Interval {

        private Calendar c;

        public void startTicking(Calendar calendar) {
            this.c = (Calendar) calendar.clone();

            c.set(Calendar.MONTH, c.getMinimum(Calendar.MONTH));
            c.set(Calendar.DAY_OF_MONTH, c.getMinimum(Calendar.DAY_OF_MONTH));
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
        }

        public long getNextTick() {
            c.add(Calendar.YEAR, 1);

            return c.getTimeInMillis();
        }

        @Override
        public long getApproxPeriod() {
            return DateUtils.DAY_IN_MILLIS * 30 * 12;
        }

        @Override
        public CharSequence getTickLabel(float scale) {
            return String.valueOf(c.get(Calendar.YEAR));
        }

        @Override
        public CharSequence getLabel(Context context, float scale) {
            return context.getText(R.string.years);
        }
    }

    public long getTime() {
        return (mEndTime - mStartTime) / 2 + mStartTime;
    }

    private void enforceLimits() {
        final long curTime = getTime();
        // ensure that the start time is never below the min time
        if (curTime < mMinTime) {
            final long minOffset = mMinTime - curTime;
            mStartTime += minOffset;
            mEndTime += minOffset;
        }

        if (mEndTime - mStartTime < MIN_SCALE) {
            final long minOffset = (MIN_SCALE - (mEndTime - mStartTime)) / 2;
            mStartTime -= minOffset;
            mEndTime += minOffset;
        }

        if (mEndTime - mStartTime > MAX_SCALE) {
            final long minOffset = ((mEndTime - mStartTime) - MAX_SCALE) / 2;
            mStartTime += minOffset;
            mEndTime -= minOffset;
        }
    }

    /**
     * Sets the current time.
     *
     * @param time
     *            the time in milliseconds
     */
    public void setTime(long time) {
        final long curtime = getTime();
        final long offset = time - curtime;
        mEndTime += offset;
        mStartTime += offset;

        // enforceMinimum();

        invalidate();
        notifyListener();
    }

    private static final int DATE_FORMAT_ARGS = DateUtils.FORMAT_SHOW_TIME
            | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR
            | DateUtils.FORMAT_SHOW_WEEKDAY;

    public static CharSequence getFormattedDateTime(Context context, long time) {
        final long fromNow = Math.abs(System.currentTimeMillis() - time);

        final StringBuilder sb = new StringBuilder();
        if (fromNow > DateUtils.DAY_IN_MILLIS) {
            sb.append(DateUtils.formatDateTime(context, time, DateUtils.FORMAT_SHOW_WEEKDAY));
            sb.append(", ");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {

            sb.append(DateUtils.getRelativeDateTimeString(context, time,
                    DateUtils.MINUTE_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, DATE_FORMAT_ARGS));
        } else {
            sb.append(info.staticfree.android.widget.text.format.DateUtils
                    .getRelativeDateTimeString(context, time, DateUtils.MINUTE_IN_MILLIS,
                            DateUtils.WEEK_IN_MILLIS, DATE_FORMAT_ARGS));
        }
        return sb.toString();
    }

    @Override
    public CharSequence getContentDescription() {

        return getFormattedDateTime(mContext, getTime());
    }

    public void setMinimumTime(long minTime) {
        mMinTime = minTime;
        invalidate();
    }

    /**
     * Sets the range. This is essentially the zoom scale.
     *
     * @param range
     *            the size of the range entry, in milliseconds
     */
    public void setRange(long range) {
        final long offset = range - (mEndTime - mStartTime) / 2;
        mEndTime += offset;
        mStartTime -= offset;

        // enforceMinimum();

        invalidate();
        notifyListener();
    }

    /**
     * Translates the timeline by a certain number of pixels.
     *
     * @param pixels
     *            the number of pixels to offset by
     */
    public void translateTimeline(float pixels) {
        final long offset = (long) (mMultX * pixels);

        mStartTime += offset;
        mEndTime += offset;

        enforceLimits();

        invalidate();
        notifyListener();
    }

    /**
     * Scales the timeline by a certain number of pixels, centered at the given center.
     *
     * @param center
     *            not currently used
     * @param pixels
     *            the number of pixels to scale it by
     */
    public void scaleTimeline(float center, float pixels) {
        final long offset = (long) (mMultX * pixels) / 2;
        mEndTime += offset;
        mStartTime -= offset;

        enforceLimits();

        invalidate();
        notifyListener();
    }

    private void notifyListener() {
        if (mOnChangeListener != null) {
            mOnChangeListener.onChange(getTime(), mStartTime, mEndTime);
        }
    }

    /**
     * Tries to claim the user's drag motion, and requests disallowing any ancestors from stealing
     * events in the drag.
     */
    private void attemptClaimDrag() {
        final ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = event.getAction();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                attemptClaimDrag();
        }

        return mGestureDetector.onTouchEvent(event);
    }

    private final OnGestureListener mGestureListener = new OnGestureListener() {

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            mHandler.sendEmptyMessage(MSG_STOP_SCROLLING);
            return true;
        }

        @Override
        public void onShowPress(MotionEvent e) {

        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

            scroll((int) e2.getX(), (int) e2.getY(), distanceX, distanceY);

            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (Math.abs(velocityX) > Math.abs(velocityY)) {
                fling((int) -velocityX, 0);
            } else {
                fling(0, (int) -(velocityY / mDisplayMetrics.density));
            }
            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }
    };

    private void fling(int velocityX, int velocityY) {
        // this compensates for the way that the scroller behaves when it's close to the edge.
        // Normally, the behavior is very unusual and causes the scroller to suddenly decrease in
        // velocity. This makes it more gradual, such that the scroller hits the edge when very
        // close. Larger flings cause the scroller to decelerate until it reaches the edge, which is
        // functionally useful for a time slider, even if physics-wise it's a little funky.
        int minX = (int) ((mMinTime - getTime()) * mScaleX) + mScrollX;
        if (mScrollX - minX < mWidth) {
            minX -= mWidth / 2;
        }
        mScroller.fling(mScrollX, mScrollY, velocityX, velocityY,
                Math.max(minX, Integer.MIN_VALUE), Integer.MAX_VALUE /* maxX */,
                Integer.MIN_VALUE /* minY */, Integer.MAX_VALUE /* maxY */);

        postInvalidate();
    }

    @Override
    public void computeScroll() {

        if (mScroller.computeScrollOffset()) {
            final int oldX = mScrollX;
            final int oldY = mScrollY;
            final int x = mScroller.getCurrX();
            final int y = mScroller.getCurrY();

            if (oldX != x || oldY != y) {
                // TODO if the center scaling is ever used, this should be updated.
                scroll(0, 0, x - oldX, y - oldY);
                mScrollX = x;
                mScrollY = y;
            }
        }
    }

    protected void scroll(int x, int y, float deltaX, float deltaY) {

        translateTimeline(deltaX);
        scaleTimeline(x, deltaY);
    }

    public interface OnChangeListener {
        public void onChange(long newValue, long min, long max);
    }
}
