package info.staticfree.android.dearfutureself;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.view.animation.DecelerateInterpolator;

public class TimelineEntry extends View {
	@SuppressWarnings("unused")
	private static final String TAG = TimelineEntry.class.getSimpleName();
	long mStartTime, mEndTime;
	long mMinTime = 0;

	private static final Paint PAINT_AXIS = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_MAJOR_TICKS = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_MINOR_TICKS = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_DISABLED = new Paint(Paint.ANTI_ALIAS_FLAG);

	// ms → s → m → h → d
	private static final int DEFAULT_SCALE = 1000 * 60 * 60 * 24;

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

	private float mPrevX;
	private float mPrevX1;

	private float mPrevY;

	private Scroller mScroller;
	private VelocityTracker mVelocityTracker;

	//@formatter:off
	private static final int
		STATE_STILL = 0,
		STATE_TRANSLATING = 1,
		STATE_SCALING = 2;
	//@formatter:on

	private int mState = STATE_STILL;
	private float mMaximumVelocity;
	private int mTouchSlop; // TODO use this
	private int mMinimumVelocity;
	private int mActivePointerId;
	private int mScrollX;
	private int mScrollY;

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

	private static final Paint RED_OUTLINE = new Paint();


	/**
	 * ms that the pointer must be held down to stop scrolling.
	 */
	private static final long STOP_SCROLLING_DELAY = 200;

	static {
		PAINT_AXIS.setARGB(192, 0, 0, 0);
		PAINT_AXIS.setStyle(Style.STROKE);
		PAINT_AXIS.setStrokeWidth(2);

		PAINT_MAJOR_TICKS.setARGB(128, 0, 0, 0);
		PAINT_MAJOR_TICKS.setStyle(Style.STROKE);
		PAINT_MAJOR_TICKS.setStrokeWidth(2);

		PAINT_MINOR_TICKS.setARGB(92, 0, 0, 0);
		PAINT_MINOR_TICKS.setStyle(Style.STROKE);
		PAINT_MINOR_TICKS.setStrokeWidth(2);

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
		mMarker = context.getResources().getDrawable(R.drawable.timeline_marker);

		mScroller = new Scroller(context, new DecelerateInterpolator(), true);
		mHandler = new ScrollHandler(mScroller);

		final ViewConfiguration configuration = ViewConfiguration.get(context);
		mTouchSlop = configuration.getScaledTouchSlop();
		mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
		mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();

		if (mStartTime == 0) {
			mEndTime = System.currentTimeMillis();

			mStartTime = mEndTime - DEFAULT_SCALE;
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

	public void setOnChangeListener(OnChangeListener onChangeListener) {
		mOnChangeListener = onChangeListener;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int measuredWidth = View.MeasureSpec.getSize(widthMeasureSpec), measuredHeight = View.MeasureSpec
				.getSize(heightMeasureSpec);

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
	}

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

		// main axis
		canvas.drawLine(0, hCenter, w, hCenter, PAINT_AXIS);

		// show the disabled region
		if (mMinTime > mStartTime) {
			canvas.drawRect(0, 0, (mMinTime - mStartTime) * mScaleX, h, PAINT_DISABLED);
		}

		// draw minutes
		if (timelineW < DateUtils.HOUR_IN_MILLIS * 6) {
			// draw minor ticks
			for (long xMarker = mStartTime - (mStartTime % DateUtils.MINUTE_IN_MILLIS); xMarker < mEndTime; xMarker += DateUtils.MINUTE_IN_MILLIS) {
					canvas.drawLine((xMarker - mStartTime) * mScaleX, hCenter,
							(xMarker - mStartTime) * mScaleX, hCenter - MINOR_TICK_SIZE,
							PAINT_MINOR_TICKS);
			}
		}

		if (timelineW < DateUtils.WEEK_IN_MILLIS) {

			// draw minor ticks
			for (long xMarker = mStartTime - (mStartTime % X_SCALE_SMALL); xMarker < mEndTime; xMarker += X_SCALE_SMALL) {
				if (xMarker % X_SCALE == 0) {
					canvas.drawLine((xMarker - mStartTime) * mScaleX, hCenter + MAJOR_TICK_SIZE,
							(xMarker - mStartTime) * mScaleX, hCenter - MAJOR_TICK_SIZE,
							PAINT_MAJOR_TICKS);
				} else {
					if (timelineW < DateUtils.DAY_IN_MILLIS) {
						canvas.drawLine((xMarker - mStartTime) * mScaleX, hCenter,
								(xMarker - mStartTime) * mScaleX, hCenter - MINOR_TICK_SIZE,
								PAINT_MINOR_TICKS);
					}
				}
			}
		}

		// final Path path = new Path();
		// XXX path.canvas.drawPath(path, PAINT_DISABLED);

		canvas.restore();

		mMarker.setBounds(mPaddingLeft, 0, w + mPaddingRight, h);
		mMarker.draw(canvas);
	}

	public long getTime() {
		return (mEndTime - mStartTime) / 2 + mStartTime;
	}

	private void enforceMinimum() {
		final long curTime = getTime();
		// ensure that the start time is never below the min time
		if (curTime < mMinTime) {
			final long minOffset = mMinTime - curTime;
			mStartTime += minOffset;
			mEndTime += minOffset;
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

		enforceMinimum();

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

		enforceMinimum();

		invalidate();
		notifyListener();
	}

	private void notifyListener() {
		if (mOnChangeListener != null) {
			mOnChangeListener.onChange(mStartTime + (mEndTime - mStartTime) / 2, mStartTime,
					mEndTime);
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

	/*
	 * Copyright (C) 2009 The Android Open Source Project
	 *
	 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
	 * except in compliance with the License. You may obtain a copy of the License at
	 *
	 * http://www.apache.org/licenses/LICENSE-2.0
	 *
	 * Unless required by applicable law or agreed to in writing, software distributed under the
	 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
	 * either express or implied. See the License for the specific language governing permissions
	 * and limitations under the License.
	 */

	private void initOrResetVelocityTracker() {
		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		} else {
			mVelocityTracker.clear();
		}
	}

	private void initVelocityTrackerIfNotExists() {
		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		}
	}

	private void recycleVelocityTracker() {
		if (mVelocityTracker != null) {
			mVelocityTracker.recycle();
			mVelocityTracker = null;
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		final int action = event.getAction();

		initVelocityTrackerIfNotExists();

		final int idx = event.getActionIndex();

		final boolean twoFingers = event.getPointerCount() == 2;

		switch (action & MotionEvent.ACTION_MASK) {
		// first finger
			case MotionEvent.ACTION_DOWN: {
				attemptClaimDrag();
				initOrResetVelocityTracker();
				mVelocityTracker.addMovement(event);
				mState = STATE_TRANSLATING;
				mPrevX = event.getX(idx);
				mPrevY = event.getY(idx);

				mHandler.sendEmptyMessageDelayed(MSG_STOP_SCROLLING, STOP_SCROLLING_DELAY);

				mActivePointerId = event.getPointerId(0);

				return true;
			}

			// second+ fingers
			case MotionEvent.ACTION_POINTER_DOWN: {
				attemptClaimDrag();

				if (twoFingers) {
					mPrevX1 = event.getX(idx);
					mState = STATE_SCALING;
					mScroller.abortAnimation();
				}
				return true;
			}

			case MotionEvent.ACTION_MOVE: {
				final float x = event.getX(idx);
				final float y = event.getY(idx);

				switch (mState) {
					case STATE_TRANSLATING:
						mVelocityTracker.addMovement(event);

						if (mScroller.isFinished()) {
							onScrollChanged((int) mPrevX, (int) mPrevY, (int) x, (int) y);
						}
						break;

					case STATE_SCALING:
						final float x1 = event.getX(1),
						x0 = event.getX(0);
						if (x1 > x0) {
							scaleTimeline(0, (mPrevX1 - x1) - (mPrevX - x0));
						} else {
							scaleTimeline(0, (mPrevX - x0) - (mPrevX1 - x1));
						}
						mPrevX1 = event.getX(1);
						break;
				}
				mPrevX = x;
				mPrevY = y;
				return true;
			}
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_POINTER_UP:
			case MotionEvent.ACTION_UP: {

				switch (mState) {
					case STATE_SCALING:
						mState = STATE_TRANSLATING;
						break;

					case STATE_TRANSLATING:
						mHandler.removeMessages(MSG_STOP_SCROLLING);

						mVelocityTracker.addMovement(event);
						mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);

						final int initialXVelocity = (int) mVelocityTracker
								.getXVelocity(mActivePointerId);

						final int initialYVelocity = (int) mVelocityTracker
								.getYVelocity(mActivePointerId);

						final int absX = Math.abs(initialXVelocity);
						final int absY = Math.abs(initialYVelocity);
						if ((absX > mMinimumVelocity) || (absY > mMinimumVelocity)) {
							if (absX > absY) {
								fling(-initialXVelocity, 0);
							} else {
								fling(0, -initialYVelocity);
							}
						}

						mState = STATE_STILL;
				}
				recycleVelocityTracker();
			}

				return true;

			default:
				return super.onTouchEvent(event);
		}

	}

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
				onScrollChanged(x, y, oldX, oldY);
			}
		}
	}

	@Override
	protected void onScrollChanged(int x, int y, int oldl, int oldt) {
		mScrollX = x;
		mScrollY = y;

		translateTimeline(x - oldl);
		scaleTimeline(mPrevX, y - oldt);
	}

	public interface OnChangeListener {
		public void onChange(long newValue, long min, long max);
	}
}
