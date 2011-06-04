package info.staticfree.android.dearfutureself;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.format.DateUtils;
import android.widget.TextView;

public class DestinationEdit extends FragmentActivity implements TimelineEntry.OnChangeListener {

	TextView mTimeView;

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		setContentView(R.layout.destination_edit);

		final TimelineEntry timeline = (TimelineEntry) findViewById(R.id.timeline);
		timeline.setOnChangeListener(this);
		mTimeView = (TextView) findViewById(R.id.time);
	}

	@Override
	public void onChange(long newValue, long min, long max) {
		//mTimeView.setText(DateUtils.formatDateRange(this, min, max, DateUtils.FORMAT_SHOW_TIME|DateUtils.FORMAT_SHOW_DATE|DateUtils.FORMAT_SHOW_YEAR));
		mTimeView.setText(DateUtils.getRelativeDateTimeString(this, newValue, DateUtils.MINUTE_IN_MILLIS, DateUtils.HOUR_IN_MILLIS, DateUtils.FORMAT_SHOW_TIME|DateUtils.FORMAT_SHOW_DATE|DateUtils.FORMAT_SHOW_YEAR));

	}
}
