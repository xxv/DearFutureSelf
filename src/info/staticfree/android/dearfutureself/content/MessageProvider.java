package info.staticfree.android.dearfutureself.content;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import edu.mit.mobile.android.content.GenericDBHelper;
import edu.mit.mobile.android.content.ProviderUtils;
import edu.mit.mobile.android.content.QuerystringWrapper;
import edu.mit.mobile.android.content.SimpleContentProvider;
import edu.mit.mobile.android.content.dbhelper.SearchDBHelper;

public class MessageProvider extends SimpleContentProvider {

    public static final String AUTHORITY = "info.staticfree.android.dearfutureself";

    public static final String SEARCH_PATH = null;
    public static final Uri SEARCH = ProviderUtils.toContentUri(AUTHORITY, SEARCH_PATH);

    public static final int DB_VER = 13;

    protected static final String SEARCH_FILTER = Message.PATH + "." + Message.STATE + "=="
            + Message.STATE_NEW + " OR " + Message.PATH + "." + Message.STATE + "=="
            + Message.STATE_READ;

    public MessageProvider() {
        super(AUTHORITY, DB_VER);

        final GenericDBHelper messageHelperRaw = new GenericDBHelper(Message.class);
        final QuerystringWrapper messageHelper = new QuerystringWrapper(messageHelperRaw);

        addDirAndItemUri(messageHelper, Message.PATH);

        final SearchDBHelper searchHelper = new SearchDBHelper() {

            @Override
            public Cursor queryDir(SQLiteDatabase db, Uri uri, String[] projection,
                    String selection, String[] selectionArgs, String sortOrder) {
                return super.queryDir(db, uri, projection,
                        ProviderUtils.addExtraWhere(selection, SEARCH_FILTER), selectionArgs,
                        sortOrder);
            }

            @Override
            public Cursor queryItem(SQLiteDatabase db, Uri uri, String[] projection,
                    String selection, String[] selectionArgs, String sortOrder) {
                return super.queryItem(db, uri, projection,
                        ProviderUtils.addExtraWhere(selection, SEARCH_FILTER), selectionArgs,
                        sortOrder);
            }
        };

        searchHelper.registerDBHelper(messageHelperRaw, Message.CONTENT_URI, Message.SUBJECT,
                Message.BODY, Message.SUBJECT, Message.BODY);

        addSearchUri(searchHelper, SEARCH_PATH);
    }
}
