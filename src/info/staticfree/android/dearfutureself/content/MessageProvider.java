package info.staticfree.android.dearfutureself.content;

import android.net.Uri;
import edu.mit.mobile.android.content.GenericDBHelper;
import edu.mit.mobile.android.content.ProviderUtils;
import edu.mit.mobile.android.content.QuerystringWrapper;
import edu.mit.mobile.android.content.SimpleContentProvider;
import edu.mit.mobile.android.content.dbhelper.SearchDBHelper;


public class MessageProvider extends SimpleContentProvider {

    public static final String
        AUTHORITY = "info.staticfree.android.dearfutureself";

    public static final String SEARCH_PATH = null;
    public static final Uri SEARCH = ProviderUtils.toContentUri(AUTHORITY, SEARCH_PATH);

    public static final int DB_VER = 13;

    public MessageProvider() {
        super(AUTHORITY, DB_VER);

        final GenericDBHelper messageHelperRaw = new GenericDBHelper(Message.class);
        final QuerystringWrapper messageHelper = new QuerystringWrapper(messageHelperRaw);

        addDirAndItemUri(messageHelper, Message.PATH);

        final SearchDBHelper searchHelper = new SearchDBHelper();

        searchHelper.registerDBHelper(messageHelperRaw, Message.CONTENT_URI, Message.SUBJECT,
                Message.BODY, Message.SUBJECT, Message.BODY);

        addSearchUri(searchHelper, SEARCH_PATH);
    }
}
