package info.staticfree.android.dearfutureself.content;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import edu.mit.mobile.android.content.DBHelperMapper;
import edu.mit.mobile.android.content.GenericDBHelper;


public class MessageProvider extends ContentProvider {

	public static final String
		AUTHORITY = "info.staticfree.android.dearfutureself";

	private static final GenericDBHelper MESSAGE_HELPER = new GenericDBHelper(Message.class, Message.CONTENT_URI);

	private static final DBHelperMapper DB_HELPER_MAPPER = new DBHelperMapper(AUTHORITY);
	private static final UriMatcher MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

	private DatabaseHelper mDatabaseHelper;

	private static class DatabaseHelper extends SQLiteOpenHelper {
		private static final String DB_NAME = "content";
		private static final int DB_VERSION = 6;


		public DatabaseHelper(Context context) {
			super(context, DB_NAME, null, DB_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			MESSAGE_HELPER.createTable(db);

		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			MESSAGE_HELPER.upgradeTable(db);
		}
	}

	@Override
	public boolean onCreate() {
		mDatabaseHelper = new DatabaseHelper(getContext());
		return true;
	}

	private static final int
		MATCHER_MESSAGE_DIR = 0,
		MATCHER_MESSAGE_ITEM = 1,
		MATCHER_DESTINATION_DIR = 2,
		MATCHER_DESTINATION_ITEM = 3;

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		final SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
		final int match = MATCHER.match(uri);
		if (!DB_HELPER_MAPPER.canDelete(match)){
			throw new IllegalArgumentException("delete note supported");
		}
		return DB_HELPER_MAPPER.delete(match, this, db, uri, selection, selectionArgs);
	}

	@Override
	public String getType(Uri uri) {
		final int match = MATCHER.match(uri);
		return DB_HELPER_MAPPER.getType(match);
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		final SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
		final int match = MATCHER.match(uri);
		if (!DB_HELPER_MAPPER.canInsert(match)){
			throw new IllegalArgumentException("insert not supported");
		}
		final Uri newUri = DB_HELPER_MAPPER.insert(match, this, db, uri, values);
		if (newUri != null){
			getContext().getContentResolver().notifyChange(uri, null);
		}
		return newUri;
	}


	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		final SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
		final int match = MATCHER.match(uri);
		if(!DB_HELPER_MAPPER.canQuery(match)){
			throw new IllegalArgumentException("query not supported");
		}
		final Cursor c = DB_HELPER_MAPPER.query(match, this, db, uri, projection, selection, selectionArgs, sortOrder);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		final SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
		final int match = MATCHER.match(uri);
		if (!DB_HELPER_MAPPER.canUpdate(match)){
			throw new IllegalArgumentException("update not supported");
		}
		final int changed = DB_HELPER_MAPPER.update(match, this, db, uri, values, selection, selectionArgs);
		if (changed != 0){
			getContext().getContentResolver().notifyChange(uri, null);
		}
		return changed;
	}

	static {
		MATCHER.addURI(AUTHORITY, Message.PATH, MATCHER_MESSAGE_DIR);
		MATCHER.addURI(AUTHORITY, Message.PATH + "/#", MATCHER_MESSAGE_ITEM);

		DB_HELPER_MAPPER.addDirMapping(MATCHER_MESSAGE_DIR, MESSAGE_HELPER, DBHelperMapper.TYPE_ALL);
		DB_HELPER_MAPPER.addItemMapping(MATCHER_MESSAGE_ITEM, MESSAGE_HELPER, DBHelperMapper.TYPE_ALL);
	}
}
