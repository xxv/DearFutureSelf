package info.staticfree.android.dearfutureself.content;

import edu.mit.mobile.android.content.GenericDBHelper;
import edu.mit.mobile.android.content.QuerystringWrapper;
import edu.mit.mobile.android.content.SimpleContentProvider;


public class MessageProvider extends SimpleContentProvider {

    public static final String
        AUTHORITY = "info.staticfree.android.dearfutureself";

    public static final int DB_VER = 13;

    public MessageProvider() {
        super(AUTHORITY, DB_VER);

        final QuerystringWrapper messageHelper = new QuerystringWrapper(new GenericDBHelper(Message.class));

        addDirAndItemUri(messageHelper, Message.PATH);
    }
}
