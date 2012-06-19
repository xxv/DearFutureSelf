package info.staticfree.android.dearfutureself.content;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.format.Time;
import android.util.Log;

public class MessageUtils {
	private static final String TAG = MessageUtils.class.getSimpleName();

	private static final String
		META_KEY = "metadata",
		META_APP_VER_CODE = "app_version_code",
		META_APP_VER = "app_version",
		META_DATABASE_VER = "db_version",
		META_EXPORT_DATE = "export_date",

		MESSAGES_KEY = "messages"
			;

	// TODO double-check that we can actually write to SD card
	public static void exportJson(Context context, String file) throws IOException, JSONException, FileNotFoundException {
		final ContentResolver cr = context.getContentResolver();

		final Cursor c = cr.query(Message.CONTENT_URI, null, null, null, null);

		final JSONObject doc = new JSONObject();
		doc.put(META_KEY, exportMetadata(context));

		final JSONArray msgs = new JSONArray();
		try {

			for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()){
				final JSONObject jo = new JSONObject();
				for (int i = 0; i < c.getColumnCount(); i++){
					final String colName = c.getColumnName(i);
//					if (BaseColumns._ID.equals(colName)){
//						continue;
//					}
					jo.put(colName, c.getString(i));

				}
				msgs.put(jo);
			}

			final File output = new File(context.getExternalFilesDir(null), file);
			output.createNewFile();
			final FileOutputStream fos = new FileOutputStream(output);

			final OutputStreamWriter osw = new OutputStreamWriter(fos);
			osw.append(msgs.toString(2));
			osw.close();
			fos.close();

		}finally{
			c.close();
		}
		doc.put(MESSAGES_KEY, msgs);
	}

	public static void importJson(Context context, String file){
		// TODO implement me
	}

	private static JSONObject exportMetadata(Context context) throws JSONException {
		final JSONObject jo = new JSONObject();

		final PackageManager pm = context.getPackageManager();

	       try {
	    	   final PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
               jo.put(META_APP_VER_CODE, pi.versionCode);
               jo.put(META_APP_VER, pi.versionName);

       } catch (final NameNotFoundException e) {
               Log.e(TAG, "Cannot get version for self! Who am I?! What's going on!? I'm so confused :-(");
       }


		jo.put(META_DATABASE_VER, MessageProvider.DB_VER);
		jo.put(META_EXPORT_DATE, new Time().format3339(false));

		return jo;
	}

	public static class SetStateTask extends AsyncTask<Object, Void, Boolean> {
		private final Context mContext;
		public SetStateTask(Context context) {
			mContext = context;
		}

		@Override
		protected Boolean doInBackground(Object... params) {
			return Message.setState(mContext.getContentResolver(), (Uri)params[0], ((Integer)params[1]).intValue());
		}
	}

}


