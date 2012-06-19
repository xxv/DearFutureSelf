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
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.markupartist.android.widget.ActionBar;


public class MessageList extends FragmentActivity implements LoaderCallbacks<Cursor>, OnClickListener, OnItemClickListener {

	private CursorAdapter mListAdapter;

	private ActionBar mActionBar;

	public static final Uri INBOX_URI = Message.CONTENT_URI.buildUpon().
		appendQueryParameter(Message.STATE, String.valueOf(Message.STATE_NEW)).
		appendQueryParameter("|"+Message.STATE, String.valueOf(Message.STATE_READ)).build();

    @Override
    public void onCreate(Bundle savedInstanceState) {
    	requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mListAdapter = new MessageListAdapter(this);

        final ListView list = (ListView) findViewById(android.R.id.list);
        list.setAdapter(mListAdapter);
        list.setEmptyView(findViewById(android.R.id.empty));
        list.setOnItemClickListener(this);
        list.setOnCreateContextMenuListener(this);

        mActionBar = (ActionBar) findViewById(R.id.actionbar);
        getMenuInflater().inflate(R.menu.message_list_options, mActionBar.asMenu());

        final Intent intent = getIntent();
        if (intent.getData() == null){
        	intent.setData(INBOX_URI);
        }
        setIntent(intent);

        getSupportLoaderManager().initLoader(0, null, this);
    }

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		mActionBar.setProgressBarVisibility(View.VISIBLE);

		return new CursorLoader(this, getIntent().getData(), MessageListAdapter.PROJECTION,
				null,
				null, Message.SORT_DEFAULT);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor c) {
		mListAdapter.swapCursor(c);
		mActionBar.setProgressBarVisibility(View.GONE);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		mListAdapter.swapCursor(null);
	}


	@Override
	public void onClick(View v) {
		switch (v.getId()){
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		getMenuInflater().inflate(R.menu.message_context, menu);
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final Uri baseUri = getIntent().getData();
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
       } catch (final ClassCastException e) {
           //Log.e(TAG, "bad menuInfo", e);
           return false;
       }
       final Uri itemUri = ContentUris.withAppendedId(baseUri, info.id);

		switch (item.getItemId()){
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
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()){
		case R.id.add:
			startActivity(new Intent(Intent.ACTION_INSERT, getIntent().getData()));
			return true;

		case R.id.import_export:
			startActivity(new Intent(this, ImportExport.class));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id) {
		startActivity(new Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(getIntent().getData(), id)));

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.message_list_options, menu);
		return super.onCreateOptionsMenu(menu);
	}

}
