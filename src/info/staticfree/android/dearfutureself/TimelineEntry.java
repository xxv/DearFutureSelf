package info.staticfree.android.dearfutureself;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;

public class TimelineEntry extends View {
	long mStartTime, mEndTime;
	long mMinTime = 0;

	private static final Paint PAINT_AXIS = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_MAJOR_TICKS = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_MINOR_TICKS = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_DISABLED = new Paint(Paint.ANTI_ALIAS_FLAG);

	private static final int MAJOR_TICK_SIZE = 20;
	private static final int MINOR_TICK_SIZE = 10;
	private final int X_SCALE = 60 * 60 * 1000;
	private final int X_SCALE_SMALL = 15 * 60 * 1000;

	private Drawable mMarker;

	private OnChangeListener mOnChangeListener;

	private int mWidth, mHeight;

	private int mPaddingLeft, mPaddingRight, mPaddingTop, mPaddingBottom;

	private float mScaleX;
	private float mMultX;
	private float mScaleCenterX;

	private float mPrevX;
	private float mPrevX1;

	private static final int
		STATE_STILL = 0,
		STATE_TRANSLATING = 1,
		STATE_SCALING = 2;
	private int  mState = STATE_STILL;

	private static final Paint RED_OUTLINE = new Paint();
	static {
		PAINT_AXIS.setARGB(192, 0, 0, 0);
		PAINT_AXIS.setStyle(Style.STROKE);
		PAINT_AXIS.setStrokeWidth(4);

		PAINT_MAJOR_TICKS.setARGB(128, 0, 0, 0);
		PAINT_MAJOR_TICKS.setStyle(Style.STROKE);
		PAINT_MAJOR_TICKS.setStrokeWidth(0);

		PAINT_MINOR_TICKS.setARGB(92, 0, 0, 0);
		PAINT_MINOR_TICKS.setStyle(Style.STROKE);
		PAINT_MINOR_TICKS.setStrokeWidth(0);

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

	private void init(Context context){
		mMarker = context.getResources().getDrawable(R.drawable.timeline_marker);

		if (mStartTime == 0){
			mEndTime = System.currentTimeMillis();
			//                 ms   →  s  → m  → h  → d
			mStartTime = mEndTime - 1000 * 60 * 60 * 24;
		}
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		final Parcelable superState = super.onSaveInstanceState();
		final SavedState ss = new SavedState(superState, mStartTime, mEndTime);
		return ss;
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		if ( !(state instanceof SavedState)){
			super.onRestoreInstanceState(state);
			return;
		}

		final SavedState ss = (SavedState)state;
		super.onRestoreInstanceState(ss.getSuperState());
		mStartTime = ss.mStartTime;
		mEndTime = ss.mEndTime;
	}

	private static class SavedState extends BaseSavedState{
		private final long mStartTime;
		private final long mEndTime;

		public SavedState(Parcelable superState, long startTime, long endTime) {
			super(superState);
			mStartTime = startTime;
			mEndTime = endTime;
		}

		public SavedState(Parcel in){
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
		public static final Parcelable.Creator<SavedState> CREATOR = new Creator<TimelineEntry.SavedState>() {

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

	public void setOnChangeListener(OnChangeListener onChangeListener){
		mOnChangeListener = onChangeListener;
	}

	@Override
	protected int getSuggestedMinimumHeight() {
		return getBackground().getIntrinsicHeight();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int measuredWidth = View.MeasureSpec.getSize(widthMeasureSpec),
			measuredHeight = View.MeasureSpec.getSize(widthMeasureSpec);

		switch (View.MeasureSpec.getMode(widthMeasureSpec)){
		case View.MeasureSpec.UNSPECIFIED:
			measuredWidth = getBackground().getIntrinsicWidth();
			break;
		case View.MeasureSpec.AT_MOST:

			break;
		case View.MeasureSpec.EXACTLY:
			// already set!
			break;
		}
		switch (View.MeasureSpec.getMode(heightMeasureSpec)){
		case View.MeasureSpec.UNSPECIFIED:
			measuredHeight = 100;
			break;
		case View.MeasureSpec.AT_MOST:
			measuredHeight = 100;
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
		mWidth = w - (mPaddingLeft);
		mHeight = h - (mPaddingTop);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		final int w = mWidth;
		final int h = mHeight;
		final int hCenter = h/2;

		final long timelineW = mEndTime - mStartTime;
		mScaleX = getWidth() / (float)timelineW;
		mMultX = (float)timelineW / getWidth();

		canvas.save();
		//canvas.clipRect(mPaddingLeft, mPaddingTop, w, h);
		// main axis
		canvas.drawLine(mPaddingLeft, h/2 + mPaddingTop, w, h/2 + mPaddingTop, PAINT_AXIS);
		//canvas.drawRect(mPaddingLeft, mPaddingTop, w, h, RED_OUTLINE);
		//final float scaleX = w/(float)timelineW;
		canvas.scale(mScaleX, 1, mScaleCenterX, 0);
		canvas.translate(-mStartTime, h/2);

		// show the disabled region
		if (mMinTime > mStartTime){
			canvas.drawRect(mPaddingLeft, mPaddingTop - h/2, mMinTime, h/2, PAINT_DISABLED);
		}

//		// draw minor ticks
//		for (long xMarker = mStartTime - (mStartTime % X_SCALE); xMarker < mEndTime; xMarker += X_SCALE){
//			canvas.drawLine(xMarker, -MAJOR_TICK_SIZE, xMarker, MAJOR_TICK_SIZE, PAINT_MAJOR_TICKS);
//		}
		// draw minor ticks
		for (long xMarker = mStartTime - (mStartTime % X_SCALE_SMALL); xMarker < mEndTime; xMarker += X_SCALE_SMALL){
			if (xMarker % X_SCALE == 0){
				canvas.drawLine(xMarker, -MAJOR_TICK_SIZE, xMarker, MAJOR_TICK_SIZE, PAINT_MAJOR_TICKS);
			}else{
				canvas.drawLine(xMarker, -MINOR_TICK_SIZE, xMarker, MINOR_TICK_SIZE, PAINT_MINOR_TICKS);
			}
		}
		canvas.restore();

		mMarker.setBounds(0,0,w, h);
		mMarker.draw(canvas);

	}

	public long getTime(){
		return (mEndTime - mStartTime) / 2 + mStartTime;
	}

	private void enforceMinimum(){
		final long curTime = getTime();
		// ensure that the start time is never below the min time
		if (curTime < mMinTime){
			final long minOffset = mMinTime - curTime;
			mStartTime += minOffset;
			mEndTime += minOffset;
		}
	}

	/**
	 * Sets the current time.
	 *
	 * @param time the time in milliseconds
	 */
	public void setTime(long time){
		final long curtime = getTime();
		final long offset = time - curtime;
		mEndTime += offset;
		mStartTime += offset;

		//enforceMinimum();

		invalidate();
		notifyListener();
	}

	public void setMinimumTime(long minTime){
		mMinTime = minTime;
		invalidate();
	}

	/**
	 * Sets the range. This is essentially the zoom scale.
	 *
	 * @param range the size of the range entry, in milliseconds
	 */
	public void setRange(long range){
		final long offset = range - (mEndTime - mStartTime) / 2;
		mEndTime += offset;
		mStartTime -= offset;

		//enforceMinimum();

		invalidate();
		notifyListener();
	}

	/**
	 * Translates the timeline by a certain number of pixels.
	 *
	 * @param pixels the number of pixels to offset by
	 */
	public void translateTimeline(float pixels){
		final long offset = (long)(mMultX * pixels);

		mStartTime += offset;
		mEndTime += offset;

		enforceMinimum();

		invalidate();
		notifyListener();
	}

	/**
	 * Scales the timeline by a certain number of pixels, centered at the given center.
	 *
	 * @param center not currently used
	 * @param pixels the number of pixels to scale it by
	 */
	public void scaleTimeline(float center, float pixels){
		final long offset = (long)(mMultX * pixels) / 2;
		mEndTime += offset;
		mStartTime -= offset;

		enforceMinimum();

		invalidate();
		notifyListener();
	}

	private void notifyListener(){
		if (mOnChangeListener != null){
			mOnChangeListener.onChange(mStartTime + (mEndTime - mStartTime)/2, mStartTime, mEndTime);
		}
	}

    /**
     * Tries to claim the user's drag motion, and requests disallowing any
     * ancestors from stealing events in the drag.
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
		final int pointerID = (action & MotionEvent.ACTION_POINTER_ID_MASK) >> MotionEvent.ACTION_POINTER_ID_SHIFT;

		switch (action & MotionEvent.ACTION_MASK){
		case MotionEvent.ACTION_DOWN:{
			attemptClaimDrag();
			mState = STATE_TRANSLATING;
			mPrevX = event.getX(0);
			//mScaleCenterX = mPrevX;

			return true;
		}

		case MotionEvent.ACTION_POINTER_DOWN:{
			attemptClaimDrag();

			if (pointerID == 1){
				mPrevX1 = event.getX(1);
				mState = STATE_SCALING;
			}
			return true;
		}

		case MotionEvent.ACTION_MOVE:{


			switch (mState){
			case STATE_TRANSLATING:
				translateTimeline(mPrevX - event.getX(0));
				break;

			case STATE_SCALING:
				final float x1 = event.getX(1), x0 = event.getX(0);
				if (x1 > x0){
					scaleTimeline(0, (mPrevX1- x1) - (mPrevX - x0) );
				}else{
					scaleTimeline(0, (mPrevX- x0) - (mPrevX1 - x1) );
				}
				mPrevX1 = event.getX(1);
				break;
			}
			mPrevX = event.getX(0);
			return true;
		}
		case MotionEvent.ACTION_POINTER_UP:
		case MotionEvent.ACTION_UP:{

			switch (mState){
			case STATE_SCALING:
				mState = STATE_TRANSLATING;
				break;

			case STATE_TRANSLATING:
				mState = STATE_STILL;
			}

		}return true;

		default:
			return super.onTouchEvent(event);
		}

	}

	public interface OnChangeListener {
		public void onChange(long newValue, long min, long max);
	}
}
