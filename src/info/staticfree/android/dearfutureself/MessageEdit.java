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
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.markupartist.android.widget.ActionBar;

public class MessageEdit extends FragmentActivity implements LoaderCallbacks<Cursor>, OnChangeListener {

	private String mAction;
	private Uri mData;

	TextView mTimelineValueView;
	private TimelineEntry mTimelineEntry;

	private EditText mSubjectView;
	private EditText mBodyView;

	private final SharedTextExtractor mSharedTextExtractor = new SharedTextExtractor();

	private ActionBar mActionBar;

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		setContentView(R.layout.message_edit);

		mTimelineValueView = (TextView) findViewById(R.id.timeline_value);
		mTimelineEntry = (TimelineEntry)findViewById(R.id.timeline);

		mTimelineEntry.setOnChangeListener(this);

		mSubjectView = (EditText)findViewById(R.id.subject);
		mBodyView = (EditText)findViewById(R.id.body);

		mActionBar = (ActionBar) findViewById(R.id.actionbar);


		getMenuInflater().inflate(R.menu.message_edit_options, mActionBar.asMenu());

		final Intent intent = getIntent();
		mAction = intent.getAction();
		mData = intent.getData();

		if (Intent.ACTION_INSERT.equals(mAction)){
			setTitle(R.string.new_message);

		}else if (Intent.ACTION_EDIT.equals(mAction)){
			setSendIndicator(false);
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

		mActionBar.setTitle(getTitle());

		mTimelineEntry.setTime(System.currentTimeMillis() + 60000);
		mTimelineEntry.setRange(1000 * 60 * 60);
		mTimelineEntry.setMinimumTime(System.currentTimeMillis());

	}

	private boolean mSendIndicator = true;

	private void setSendIndicator(boolean send){
		if (send == mSendIndicator){
			return;
		}
		mSendIndicator = send;

		final Menu actionMenu = mActionBar.asMenu();
		actionMenu.findItem(R.id.send).setVisible(send);
		actionMenu.findItem(R.id.save).setVisible(!send);
	}

	private ContentValues toCV(){
		final ContentValues cv = new ContentValues();
		cv.put(Message.BODY, ((EditText)findViewById(R.id.body)).getText().toString());
		cv.put(Message.SUBJECT, ((EditText)findViewById(R.id.subject)).getText().toString());
		cv.put(Message.DATE_ARRIVE, mTimelineEntry.getTime());
		return cv;
	}

	private boolean validate(){
		if (mSubjectView.getText().length() == 0){
			mSubjectView.setError("Please enter a message");
			mSubjectView.requestFocus();
			return false;
		}
		return true;
	}

	private boolean sendOrEdit(){
		boolean success = false;
		Uri message = null;
		final ContentResolver cr = getContentResolver();

		if (!validate()){
			return false;
		}

		boolean send = false;

		final ContentValues cv = toCV();

		if (Intent.ACTION_INSERT.equals(mAction) || Intent.ACTION_SEND.equals(mAction)){
			cv.put(Message.STATE, Message.STATE_IN_TRANSIT);
			send = true;
			message = cr.insert(mData, cv);
			success = message  != null;

		}else if (Intent.ACTION_EDIT.equals(mAction)){
			if (mTimelineEntry.getTime() > System.currentTimeMillis()){
				cv.put(Message.STATE, Message.STATE_IN_TRANSIT);
				send = true;
			}
			success = cr.update(mData, cv, null, null) != 0;

			message = mData;
		}
		if (success && message != null && send){
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
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()){
		case R.id.save:
		case R.id.send:
			if(sendOrEdit()){
				if (Intent.ACTION_INSERT.equals(mAction)){
					Toast.makeText(this, "Sent!", Toast.LENGTH_SHORT).show();
				}
				setResult(RESULT_OK);
				finish();
			}else{
				Toast.makeText(this, "error sending.", Toast.LENGTH_LONG).show();
			}
			return true;

		case R.id.cancel:
			setResult(RESULT_CANCELED);
			finish();
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onChange(long newValue, long min, long max) {
		if (newValue >= System.currentTimeMillis()){
			setSendIndicator(true);
		}else{
			setSendIndicator(false);
		}
		mTimelineValueView.setText(DateUtils.getRelativeDateTimeString(this, newValue, DateUtils.MINUTE_IN_MILLIS, DateUtils.HOUR_IN_MILLIS, DateUtils.FORMAT_SHOW_TIME|DateUtils.FORMAT_SHOW_DATE|DateUtils.FORMAT_SHOW_YEAR));

	}
}
