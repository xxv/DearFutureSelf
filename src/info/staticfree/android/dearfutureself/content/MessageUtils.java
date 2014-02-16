package info.staticfree.android.dearfutureself.content;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.format.Time;
import android.util.Log;
import edu.mit.mobile.android.content.SQLGenerationException;
import edu.mit.mobile.android.content.annotation.SQLExtractor;
import edu.mit.mobile.android.content.column.DBColumnType;
import edu.mit.mobile.android.content.column.DatetimeColumn;
import edu.mit.mobile.android.content.column.IntegerColumn;
import edu.mit.mobile.android.content.column.TextColumn;
import edu.mit.mobile.android.utils.StreamUtils;

public class MessageUtils {
    private static final String TAG = MessageUtils.class.getSimpleName();

    private static final String META_KEY = "metadata", META_APP_VER_CODE = "app_version_code",
            META_APP_VER = "app_version", META_DATABASE_VER = "db_version",
            META_EXPORT_DATE = "export_date",

            MESSAGES_KEY = "messages";

    private static final String[] INTENT_PROJECTION = new String[] { Message.SUBJECT, Message.BODY };

    // TODO double-check that we can actually write to SD card
    public static void exportJson(Context context, String file) throws IOException,
            ImportExportException, FileNotFoundException {
        final ContentResolver cr = context.getContentResolver();

        final Cursor c = cr.query(Message.CONTENT_URI, null, null, null, null);

        final JSONObject doc = new JSONObject();
        try {
            doc.put(META_KEY, exportMetadata(context));

            final int TYPE_STRING = 100, TYPE_DATETIME = 101, TYPE_INTEGER = 102;

            final int colCount = c.getColumnCount();

            final Map<String, Integer> types = new HashMap<String, Integer>(colCount);

            final SQLExtractor extractor = new SQLExtractor(Message.class);

            for (final Field field : Message.class.getFields()) {
                int type = 0;

                try {
                    final Class<? extends DBColumnType<?>> col = extractor.getFieldType(field);
                    if (col == null) {
                        continue;
                    }

                    if (TextColumn.class.equals(col)) {
                        type = TYPE_STRING;
                    } else if (IntegerColumn.class.equals(col)) {
                        type = TYPE_INTEGER;
                    } else if (DatetimeColumn.class.equals(col)) {
                        type = TYPE_DATETIME;
                    }

                    types.put(extractor.getDbColumnName(field), type);

                } catch (final SQLGenerationException e) {
                    throw new ImportExportException("error in Message data definition", e);
                }
            }

            final JSONArray msgs = new JSONArray();
            try {

                for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                    final JSONObject jo = new JSONObject();
                    for (int i = 0; i < c.getColumnCount(); i++) {
                        final String colName = c.getColumnName(i);

                        switch (types.get(colName)) {
                            case TYPE_DATETIME:
                                jo.put(colName, c.getLong(i));
                                break;

                            case TYPE_INTEGER:
                                jo.put(colName, c.getInt(i));
                                break;

                            case TYPE_STRING:
                                jo.put(colName, c.getString(i));
                                break;
                        }
                    }
                    msgs.put(jo);
                }

                final File output = new File(file);
                output.createNewFile();
                final FileOutputStream fos = new FileOutputStream(output);

                final OutputStreamWriter osw = new OutputStreamWriter(fos);
                osw.append(msgs.toString(2));
                osw.close();
                fos.close();

            } finally {
                c.close();
            }
            doc.put(MESSAGES_KEY, msgs);
        } catch (final JSONException e) {
            throw new ImportExportException("error encoding export (this shouldn't happen)", e);
        }
    }

    /**
     * @param context
     * @param file
     * @return the number of imported messages
     * @throws IOException
     * @throws JSONException
     */
    public static int importJson(Context context, File file) throws IOException, JSONException {
        final FileInputStream fis = new FileInputStream(file);
        final InputStreamReader isr = new InputStreamReader(fis);
        try {
            final String jsonString = StreamUtils.inputStreamToString(fis);
            final JSONArray ja = new JSONArray(jsonString);
            final int size = ja.length();
            final ContentValues cvs[] = new ContentValues[size];
            for (int i = 0; i < size; i++) {
                cvs[i] = new ContentValues();
                final JSONObject jo = ja.getJSONObject(i);

                // the JSON library really should have fixed this
                for (@SuppressWarnings("unchecked")
                final Iterator<String> iter = jo.keys(); iter.hasNext();) {
                    final String key = iter.next();
                    if (Message._ID.equals(key)) {
                        continue;
                    }

                    cvs[i].put(key, jo.getString(key));
                }
            }
            final ContentResolver cr = context.getContentResolver();
            int count = 0;

            // if the import has no content, don't trash the existing content.
            if (cvs.length > 0) {
                cr.delete(Message.CONTENT_URI, null, null);
                count = cr.bulkInsert(Message.CONTENT_URI, cvs);
            }

            return count;
        } finally {
            isr.close();
        }
    }

    private static JSONObject exportMetadata(Context context) throws JSONException {
        final JSONObject jo = new JSONObject();

        final PackageManager pm = context.getPackageManager();

        try {
            final PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            jo.put(META_APP_VER_CODE, pi.versionCode);
            jo.put(META_APP_VER, pi.versionName);

        } catch (final NameNotFoundException e) {
            Log.e(TAG,
                    "Cannot get version for self! Who am I?! What's going on!? I'm so confused :-(");
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
            return Message.setState(mContext.getContentResolver(), (Uri) params[0],
                    ((Integer) params[1]).intValue());
        }
    }

    public static Intent toShareIntent(Context context, Uri message) {
        final Cursor c = context.getContentResolver().query(message, INTENT_PROJECTION, null, null,
                null);
        try {
            if (c.moveToFirst()) {
                final Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_TEXT, c.getString(c.getColumnIndex(Message.BODY)));
                i.putExtra(Intent.EXTRA_SUBJECT, c.getString(c.getColumnIndex(Message.SUBJECT)));
                return Intent.createChooser(i, "Share with");
            } else {
                throw new IllegalArgumentException("could not extract message from provided uri");
            }
        } finally {
            c.close();
        }
    }
}
