package info.staticfree.android.dearfutureself.content;

import edu.mit.mobile.android.content.QuerystringDBHelper;
import edu.mit.mobile.android.content.SimpleContentProvider;


public class MessageProvider extends SimpleContentProvider {

	public static final String
		AUTHORITY = "info.staticfree.android.dearfutureself";

	public MessageProvider() {
		super(AUTHORITY, 13);

		final QuerystringDBHelper messageHelper = new QuerystringDBHelper(Message.class, Message.CONTENT_URI);

		addDBHelper(messageHelper);
		addDirAndItemUri(messageHelper, Message.PATH);
	}
}
