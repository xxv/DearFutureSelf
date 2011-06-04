package info.staticfree.android.dearfutureself;

import info.staticfree.android.dearfutureself.content.Message;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MessageEdit extends FragmentActivity implements LoaderCallbacks<Cursor>, OnClickListener {

	private String mAction;
	private Uri mData;

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		setContentView(R.layout.message_edit);

		((Button)findViewById(R.id.done)).setOnClickListener(this);
		((Button)findViewById(R.id.cancel)).setOnClickListener(this);

		final Intent intent = getIntent();
		mAction = intent.getAction();
		mData = intent.getData();

		if (Intent.ACTION_INSERT.equals(mAction)){

		}else if (Intent.ACTION_EDIT.equals(mAction)){

		}
	}

	private ContentValues toCV(){
		final ContentValues cv = new ContentValues();
		cv.put(Message.BODY, ((EditText)findViewById(R.id.body)).getText().toString());
		cv.put(Message.SUBJECT, ((EditText)findViewById(R.id.subject)).getText().toString());
		return cv;
	}

	private boolean send(){
		final ContentResolver cr = getContentResolver();

		if (Intent.ACTION_INSERT.equals(mAction)){
			return cr.insert(mData, toCV()) != null;

		}else if (Intent.ACTION_EDIT.equals(mAction)){
			return cr.update(mData, toCV(), null, null) != 0;
		}

		return false;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onClick(View v) {
		switch (v.getId()){
		case R.id.done:
			if(send()){
				Toast.makeText(this, "Sent!", Toast.LENGTH_SHORT).show();
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
}
