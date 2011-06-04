package info.staticfree.android.dearfutureself;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class TimelineEntry extends View {
	long mStartTime, mEndTime;

	private static final Paint PAINT_AXIS = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_MAJOR_TICKS = new Paint(Paint.ANTI_ALIAS_FLAG);

	private static final int MAJOR_TICK_SIZE = 20;
	private final int X_SCALE = 60 * 60 * 1000;
	//private final int X_SCALE = 1;

	private Drawable mMarker;

	private OnChangeListener mOnChangeListener;

	static {
		PAINT_AXIS.setARGB(192, 0, 0, 0);
		PAINT_AXIS.setStyle(Style.STROKE);
		PAINT_AXIS.setStrokeWidth(4);

		PAINT_MAJOR_TICKS.setARGB(127, 0, 0, 0);
		PAINT_MAJOR_TICKS.setStyle(Style.STROKE);
		PAINT_MAJOR_TICKS.setStrokeWidth(0);
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
		//mStartTime = 0;
		//mEndTime = 10;

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

	public static class SavedState extends BaseSavedState{
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
		return 50;
	}

//	private float scale(long srcMin, long srcMax, long dstMin, long dstMax, long val){
//
//		return (val - srcMin) (dstMax - dstMin)
//	}

	private float mScaleX;
	private float mMultX;
	private float mScaleCenterX;


	@Override
	protected void onDraw(Canvas canvas) {
		final int w = getWidth();
		final int h = getHeight();

		final long timelineW = mEndTime - mStartTime;
		mScaleX = getWidth() / (float)timelineW;
		mMultX = (float)timelineW / getWidth();

		canvas.save();
		canvas.drawLine(0, h/2, w, h/2, PAINT_AXIS);
		//final float scaleX = w/(float)timelineW;
		canvas.scale(mScaleX, 1, mScaleCenterX, 0);
		canvas.translate(-mStartTime, h/2);

		for (long xMarker = mStartTime - (mStartTime % X_SCALE); xMarker < mEndTime; xMarker += X_SCALE){
			canvas.drawLine(xMarker, 0, xMarker, MAJOR_TICK_SIZE, PAINT_MAJOR_TICKS);
		}
		canvas.restore();

		mMarker.setBounds(0,0,w, h);
		mMarker.draw(canvas);

	}

	public void translateTimeline(float pixels){
		final long offset = (long)(mMultX * pixels);
		mStartTime += offset;
		mEndTime += offset;
		postInvalidate();
		notifyListener();
	}

	public void scaleTimeline(float center, float pixels){
		final long offset = (long)(mMultX * pixels) / 2;
		mEndTime += offset;
		mStartTime -= offset;
		postInvalidate();
		notifyListener();
	}

	private void notifyListener(){
		if (mOnChangeListener != null){
			mOnChangeListener.onChange(mStartTime + (mEndTime - mStartTime)/2, mStartTime, mEndTime);
		}
	}

	private float mPrevX;
	private float mPrevX1;
	private static final int
		STATE_STILL = 0,
		STATE_TRANSLATING = 1,
		STATE_SCALING = 2;
	private int  mState = STATE_STILL;


	@Override
	public boolean onTouchEvent(MotionEvent event) {
		final int action = event.getAction();
		final int pointerID = (action & MotionEvent.ACTION_POINTER_ID_MASK) >> MotionEvent.ACTION_POINTER_ID_SHIFT;

		switch (action & MotionEvent.ACTION_MASK){
		case MotionEvent.ACTION_DOWN:{
			mState = STATE_TRANSLATING;
			mPrevX = event.getX(0);
			//mScaleCenterX = mPrevX;

			return true;
		}

		case MotionEvent.ACTION_POINTER_DOWN:{
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
