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
import android.widget.TextView;

import com.actionbarsherlock.ActionBarSherlock;
import com.actionbarsherlock.ActionBarSherlock.OnCreateOptionsMenuListener;
import com.actionbarsherlock.ActionBarSherlock.OnOptionsItemSelectedListener;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class MessageView extends FragmentActivity implements LoaderCallbacks<Cursor>,
        OnCreateOptionsMenuListener, OnOptionsItemSelectedListener {

    private int mMessageState;

    private final ActionBarSherlock mSherlock = ActionBarSherlock.wrap(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        mSherlock.setContentView(R.layout.message_view);

        getSupportLoaderManager().initLoader(0, null, this);

    }

    @Override
    protected void onResume() {
        super.onResume();

        MessageService.markLastViewed(getApplicationContext());
    }

    private void loadFromCursor(Cursor c) {
        if (!c.moveToFirst()) {
            return;
        }
        final String subject = c.getString(c.getColumnIndex(Message.SUBJECT));
        final String body = c.getString(c.getColumnIndex(Message.BODY));
        ((TextView) findViewById(R.id.subject)).setText(subject);
        ((TextView) findViewById(R.id.body)).setText(body);

        ((TextView) findViewById(R.id.sent_time)).setText(getString(R.string.sent_x, DateUtils
                .getRelativeDateTimeString(this, c.getLong(c.getColumnIndex(Message.DATE_SENT)),
                        DateUtils.SECOND_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, 0)));

        mMessageState = c.getInt(c.getColumnIndex(Message.STATE));

        if (mMessageState == Message.STATE_NEW) {
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
        mSherlock.getMenuInflater().inflate(R.menu.message_view_options, menu);
        menu.findItem(R.id.view).setVisible(false); // already viewing!
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final Uri message = getIntent().getData();

        switch (item.getItemId()) {
            case R.id.edit:
                startActivity(new Intent(Intent.ACTION_EDIT, message));
                finish();
                return true;

            case R.id.delete:
                new SetStateTask(this).execute(message, Message.STATE_DELETED);
                finish();
                return true;

            case R.id.share:
                startActivity(MessageUtils.toShareIntent(this, message));
                return true;

            case R.id.mark_unread:
                new SetStateTask(this).execute(message, Message.STATE_NEW);
                return true;

            default:
                return false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        return mSherlock.dispatchCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(android.view.Menu menu) {
        return mSherlock.dispatchPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        return mSherlock.dispatchOptionsItemSelected(item);
    }
}
