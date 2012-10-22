package info.staticfree.android.dearfutureself;

import info.staticfree.android.dearfutureself.content.Message;
import info.staticfree.android.dearfutureself.content.MessageUtils;
import info.staticfree.android.dearfutureself.content.MessageUtils.SetStateTask;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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

    private static final int MSG_SHOW_INDETERMINATE = 100;
    private static final int SHOW_INDETERMINATE_DELAY = 200; // ms

    // loader arguments
    private static final String ARG_SORT_ORDER = "SORT_ORDER";
    private static final String ARG_URI = "URI";

    private static class MessageListHandler extends Handler {
        private final ActionBarSherlock mSherlock;

        public MessageListHandler(ActionBarSherlock sherlock) {
            mSherlock = sherlock;
        }

        @Override
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case MSG_SHOW_INDETERMINATE:
                    mSherlock.setProgressBarIndeterminateVisibility(true);
                    break;
            }
        };
    }

    private final Handler mHandler = new MessageListHandler(mSherlock);

    private final OnCloseListener mOnSearchCloseListener = new OnCloseListener() {

        @Override
        public boolean onClose() {
            onNewIntent(new Intent(Intent.ACTION_VIEW, INBOX_URI));
            return false;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mSherlock.requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        super.onCreate(savedInstanceState);
        mSherlock.setUiOptions(ActivityInfo.UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW);
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

        Bundle args = intent.getExtras();
        if (args == null) {
            args = new Bundle();
        }
        args.putParcelable(ARG_URI, intent.getData());
        getSupportLoaderManager().restartLoader(0, args, this);
    }

    void setData(Uri data) {
        setData(data, null);
    }

    void setData(Uri data, String sortOrder) {
        final Intent i = getIntent();
        i.setData(data);
        i.putExtra(ARG_SORT_ORDER, sortOrder);

        onNewIntent(i);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.d(TAG, "onCreateLoader");
        mHandler.sendEmptyMessageDelayed(MSG_SHOW_INDETERMINATE, SHOW_INDETERMINATE_DELAY);

        findViewById(android.R.id.empty).setVisibility(View.INVISIBLE);

        String sortOrder = null;
        Uri uri;
        if (args != null) {
            sortOrder = args.getString(ARG_SORT_ORDER);
            uri = args.getParcelable(ARG_URI);
        } else {
            uri = getIntent().getData();
        }

        return new CursorLoader(this, uri, MessageListAdapter.PROJECTION, null, null, sortOrder);
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
        mHandler.removeMessages(MSG_SHOW_INDETERMINATE);
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

            case R.id.share:
                startActivity(MessageUtils.toShareIntent(this, itemUri));
                return true;

            case R.id.delete:
                new SetStateTask(this).execute(itemUri, Message.STATE_DELETED);
                return true;

            case R.id.mark_unread:
                new SetStateTask(this).execute(itemUri, Message.STATE_NEW);
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
