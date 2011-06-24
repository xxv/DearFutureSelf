package info.staticfree.android.dearfutureself;

import info.staticfree.android.dearfutureself.content.Message;
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
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;


public class MessageList extends FragmentActivity implements LoaderCallbacks<Cursor>, OnClickListener, OnItemClickListener {

	private CursorAdapter mListAdapter;

	private static final Uri INBOX_URI = Message.CONTENT_URI.buildUpon().
		appendQueryParameter(Message.STATE, String.valueOf(Message.STATE_NEW)).
		appendQueryParameter("|"+Message.STATE, String.valueOf(Message.STATE_READ)).build();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mListAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_2, null,
        		new String[]{Message.SUBJECT, Message.BODY},
        		new int[]{android.R.id.text1, android.R.id.text2}, 0);

        final ListView list = (ListView) findViewById(android.R.id.list);
        list.setAdapter(mListAdapter);
        list.setOnItemClickListener(this);
        list.setOnCreateContextMenuListener(this);

        final Intent intent = getIntent();
        if (intent.getData() == null){
        	intent.setData(Message.CONTENT_URI);
        }
        setIntent(intent);

        findViewById(R.id.new_message).setOnClickListener(this);
        getSupportLoaderManager().initLoader(0, null, this);
    }

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		return new CursorLoader(this, INBOX_URI, null,
				null,
				null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor c) {
		mListAdapter.swapCursor(c);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		mListAdapter.swapCursor(null);
	}


	@Override
	public void onClick(View v) {
		switch (v.getId()){
		case R.id.new_message:
			startActivity(new Intent(Intent.ACTION_INSERT, Message.CONTENT_URI));
			break;
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

			default:
			return super.onContextItemSelected(item);
		}

	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id) {
		startActivity(new Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(getIntent().getData(), id)));

	}
}