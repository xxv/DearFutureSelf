package info.staticfree.android.dearfutureself;

import info.staticfree.android.dearfutureself.content.Message;
import info.staticfree.android.dearfutureself.content.MessageUtils;
import info.staticfree.android.dearfutureself.content.MessageUtils.SetStateTask;
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
import android.view.Window;
import android.widget.TextView;

import com.markupartist.android.widget.ActionBar;

public class MessageView extends FragmentActivity implements LoaderCallbacks<Cursor> {

	private int mMessageState;

	private ActionBar mActionBar;
	private ActionBar.Action mShareAction;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.message_view);

		getSupportLoaderManager().initLoader(0, null, this);

		mActionBar = (ActionBar) findViewById(R.id.actionbar);
		mActionBar.setTitle(getTitle());

		getMenuInflater().inflate(R.menu.message_view_options, mActionBar.asMenu());

	}

	private Intent createShareIntent() {
		final Intent i = new Intent(Intent.ACTION_SEND);
		i.setType("text/plain");
		i.putExtra(Intent.EXTRA_TEXT, ((TextView)findViewById(R.id.body)).getText());
		i.putExtra(Intent.EXTRA_SUBJECT, ((TextView)findViewById(R.id.subject)).getText());
		return Intent.createChooser(i, "Share with");
	}

	private void loadFromCursor(Cursor c){
		if (!c.moveToFirst()){
			return;
		}
		final String subject = c.getString(c.getColumnIndex(Message.SUBJECT));
		final String body = c.getString(c.getColumnIndex(Message.BODY));
		((TextView)findViewById(R.id.subject)).setText(subject);
		((TextView)findViewById(R.id.body)).setText(body);

		((TextView)findViewById(R.id.sent_time)).setText(getString(R.string.sent_x, DateUtils.getRelativeDateTimeString(this, c.getLong(c.getColumnIndex(Message.DATE_SENT)), DateUtils.SECOND_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, 0)));

		mMessageState = c.getInt(c.getColumnIndex(Message.STATE));

		if (mMessageState == Message.STATE_NEW){
			new MessageUtils.SetStateTask(this).execute(getIntent().getData(), Message.STATE_READ);
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.message_context, menu);
		menu.findItem(R.id.view).setVisible(false); // already viewing!
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final Uri message = getIntent().getData();

		switch (item.getItemId()){
		case R.id.edit:
			startActivity(new Intent(Intent.ACTION_EDIT, message));
			finish();
			return true;

		case R.id.delete:
			new SetStateTask(this).execute(message, Message.STATE_DELETED);
			finish();
			return true;

		case R.id.share:
			startActivity(createShareIntent());
			return true;

			default:
				return super.onOptionsItemSelected(item);
		}

	}
}
