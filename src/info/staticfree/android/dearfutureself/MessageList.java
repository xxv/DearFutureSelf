package info.staticfree.android.dearfutureself;

import info.staticfree.android.dearfutureself.content.Message;
import info.staticfree.android.dearfutureself.content.MessageUtils.SetStateTask;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.ActionBarSherlock;
import com.actionbarsherlock.ActionBarSherlock.OnCreateOptionsMenuListener;
import com.actionbarsherlock.ActionBarSherlock.OnOptionsItemSelectedListener;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class MessageList extends FragmentActivity implements LoaderCallbacks<Cursor>,
        OnClickListener, OnItemClickListener, OnOptionsItemSelectedListener,
        OnCreateOptionsMenuListener {

    private CursorAdapter mListAdapter;

    private final ActionBarSherlock mSherlock = ActionBarSherlock.wrap(this);

    private ListView mList;

    public static final Uri INBOX_URI = Message.getUriForStates(Message.STATE_NEW,
            Message.STATE_READ);

    private static final String TAG = MessageList.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mSherlock.requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        mSherlock.setContentView(R.layout.main);

        // re-applied here, as the label for the launcher is shorter
        setTitle(R.string.app_name);

        mListAdapter = new MessageListAdapter(this);

        mList = (ListView) findViewById(android.R.id.list);
        mList.setAdapter(mListAdapter);
        mList.setOnItemClickListener(this);
        mList.setOnCreateContextMenuListener(this);
        mList.setEmptyView(findViewById(android.R.id.empty));

        onNewIntent(getIntent());

        startService(MessageService.getScheduleIntent());

    }

    @Override
    protected void onNewIntent(Intent intent) {
        // sanity-check the intents
        if (intent.getData() == null) {
            intent.setData(INBOX_URI);
        }

        final String action = intent.getAction();

        if (!Intent.ACTION_MAIN.equals(action)
                && !(Intent.ACTION_VIEW.equals(action) && Message.CONTENT_TYPE_DIR.equals(intent
                        .resolveType(this)))) {
            Toast.makeText(this, "This application can't handle the provided intent.",
                    Toast.LENGTH_LONG).show();
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        setIntent(intent);

        getSupportLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        Log.d(TAG, "onCreateLoader");
        mSherlock.setProgressBarIndeterminateVisibility(true);

        findViewById(android.R.id.empty).setVisibility(View.INVISIBLE);

        return new CursorLoader(this, getIntent().getData(), MessageListAdapter.PROJECTION, null,
                null, Message.SORT_DEFAULT);
    }

    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        mSherlock.dispatchTitleChanged(title, color);
        super.onTitleChanged(title, color);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> arg0, Cursor c) {
        Log.d(TAG, "onLoadFinished");
        mListAdapter.swapCursor(c);
        mSherlock.setProgressBarIndeterminateVisibility(false);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        mListAdapter.swapCursor(null);
        mList.setEmptyView(null);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        getMenuInflater().inflate(R.menu.message_view_options, menu);
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        final Uri baseUri = getIntent().getData();
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (final ClassCastException e) {
            // Log.e(TAG, "bad menuInfo", e);
            return false;
        }
        final Uri itemUri = ContentUris.withAppendedId(baseUri, info.id);

        switch (item.getItemId()) {
            case R.id.view:
                startActivity(new Intent(Intent.ACTION_VIEW, itemUri));
                return true;

            case R.id.edit:
                startActivity(new Intent(Intent.ACTION_EDIT, itemUri));
                return true;

            case R.id.delete:
                new SetStateTask(this).execute(itemUri, Message.STATE_DELETED);
                return true;

            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        return mSherlock.dispatchOptionsItemSelected(item);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add:
                startActivity(new Intent(Intent.ACTION_INSERT, getIntent().getData()));
                return true;

            case R.id.import_export:
                startActivity(new Intent(this, ImportExport.class));
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id) {
        startActivity(new Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(getIntent()
                .getData(), id)));

    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        return mSherlock.dispatchCreateOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mSherlock.getMenuInflater().inflate(R.menu.message_list_options, menu);
        return true;
    }

}
