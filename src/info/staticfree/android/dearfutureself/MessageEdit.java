package info.staticfree.android.dearfutureself;

import info.staticfree.android.dearfutureself.TimelineEntry.OnChangeListener;
import info.staticfree.android.dearfutureself.content.Message;
import info.staticfree.android.dearfutureself.sharedtext.SharedTextExtractor;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MessageEdit extends FragmentActivity implements LoaderCallbacks<Cursor>, OnClickListener, OnChangeListener {

	private String mAction;
	private Uri mData;

	TextView mTimelineValueView;
	private TimelineEntry mTimelineEntry;

	private EditText mSubjectView;
	private EditText mBodyView;

	private final SharedTextExtractor mSharedTextExtractor = new SharedTextExtractor();

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		setContentView(R.layout.message_edit);

		final Button doneButton = (Button)findViewById(R.id.done);
		doneButton.setOnClickListener(this);
		((Button)findViewById(R.id.cancel)).setOnClickListener(this);
		mTimelineValueView = (TextView) findViewById(R.id.timeline_value);
		mTimelineEntry = (TimelineEntry)findViewById(R.id.timeline);

		mTimelineEntry.setOnChangeListener(this);

		mSubjectView = (EditText)findViewById(R.id.subject);
		mBodyView = (EditText)findViewById(R.id.body);

		final Intent intent = getIntent();
		mAction = intent.getAction();
		mData = intent.getData();

		if (Intent.ACTION_INSERT.equals(mAction)){

		}else if (Intent.ACTION_EDIT.equals(mAction)){
			doneButton.setText("Done");
			getSupportLoaderManager().initLoader(0, null, this);

		}else if (Intent.ACTION_SEND.equals(mAction)){
			final String body = intent.getStringExtra(Intent.EXTRA_TEXT);
			if (mSharedTextExtractor.parse(body)){
				mSubjectView.setText(mSharedTextExtractor.getSubject());
				mBodyView.setText(mSharedTextExtractor.getBody());
			}else{
				mSubjectView.setText(intent.getStringExtra(Intent.EXTRA_SUBJECT));
				mBodyView.setText(body);
			}
			mData = Message.CONTENT_URI;
		}

		mTimelineEntry.setTime(System.currentTimeMillis() + 60000);
		mTimelineEntry.setRange(1000 * 60 * 60);
		mTimelineEntry.setMinimumTime(System.currentTimeMillis());

	}

	private ContentValues toCV(){
		final ContentValues cv = new ContentValues();
		cv.put(Message.BODY, ((EditText)findViewById(R.id.body)).getText().toString());
		cv.put(Message.SUBJECT, ((EditText)findViewById(R.id.subject)).getText().toString());
		cv.put(Message.DATE_ARRIVE, mTimelineEntry.getTime());
		return cv;
	}

	private boolean sendOrEdit(){
		boolean success = false;
		Uri message = null;
		final ContentResolver cr = getContentResolver();

		if (Intent.ACTION_INSERT.equals(mAction) || Intent.ACTION_SEND.equals(mAction)){
			final ContentValues cv = toCV();
			cv.put(Message.STATE, Message.STATE_IN_TRANSIT);
			message = cr.insert(mData, cv);
			success = message  != null;

		}else if (Intent.ACTION_EDIT.equals(mAction)){
			final ContentValues cv = toCV();
			if (mTimelineEntry.getTime() > System.currentTimeMillis()){
				cv.put(Message.STATE, Message.STATE_IN_TRANSIT);
			}
			success = cr.update(mData, cv, null, null) != 0;

			message = mData;
		}
		if (success && message != null){
			startService(new Intent(MessageService.ACTION_SCHEDULE_MESSAGE, message));
		}
		return success;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		return new CursorLoader(this, mData, null, null, null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor c) {
		loadFromCursor(c);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {}

	private void loadFromCursor(Cursor c){
		if (c.moveToFirst()){
			((TextView)findViewById(R.id.subject)).setText(c.getString(c.getColumnIndex(Message.SUBJECT)));
			((TextView)findViewById(R.id.body)).setText(c.getString(c.getColumnIndex(Message.BODY)));
			((TimelineEntry)findViewById(R.id.timeline)).setTime(c.getLong(c.getColumnIndex(Message.DATE_ARRIVE)));
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()){
		case R.id.done:
			if(sendOrEdit()){
				if (Intent.ACTION_INSERT.equals(mAction)){
					Toast.makeText(this, "Sent!", Toast.LENGTH_SHORT).show();
				}
				setResult(RESULT_OK);
				finish();
			}else{
				Toast.makeText(this, "error sending.", Toast.LENGTH_LONG).show();
			}
			break;

		case R.id.cancel:
			setResult(RESULT_CANCELED);
			finish();
			break;

		}
	}

	@Override
	public void onChange(long newValue, long min, long max) {
		//mTimeView.setText(DateUtils.formatDateRange(this, min, max, DateUtils.FORMAT_SHOW_TIME|DateUtils.FORMAT_SHOW_DATE|DateUtils.FORMAT_SHOW_YEAR));
		mTimelineValueView.setText(DateUtils.getRelativeDateTimeString(this, newValue, DateUtils.MINUTE_IN_MILLIS, DateUtils.HOUR_IN_MILLIS, DateUtils.FORMAT_SHOW_TIME|DateUtils.FORMAT_SHOW_DATE|DateUtils.FORMAT_SHOW_YEAR));

	}
}
