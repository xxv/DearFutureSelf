package info.staticfree.android.dearfutureself;

import info.staticfree.android.dearfutureself.content.Message;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.widget.TextView;

public class MessageView extends FragmentActivity implements LoaderCallbacks<Cursor> {

	private int mMessageState;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.message_view);

		getSupportLoaderManager().initLoader(0, null, this);
	}

	private void loadFromCursor(Cursor c){
		if (!c.moveToFirst()){
			return;
		}
		((TextView)findViewById(R.id.subject)).setText(c.getString(c.getColumnIndex(Message.SUBJECT)));
		((TextView)findViewById(R.id.body)).setText(c.getString(c.getColumnIndex(Message.BODY)));
		mMessageState = c.getInt(c.getColumnIndex(Message.STATE));

		// mark it as read
		if (mMessageState == Message.STATE_NEW){
			final ContentValues cv = new ContentValues();
			cv.put(Message.STATE, Message.STATE_READ);
			getContentResolver().update(getIntent().getData(), cv, null, null);
		}

	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		return new CursorLoader(this, getIntent().getData(), null, null, null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor c) {
		loadFromCursor(c);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {

	}
}
