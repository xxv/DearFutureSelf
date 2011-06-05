package info.staticfree.android.dearfutureself.content;

import edu.mit.mobile.android.content.DBHelperMapper;
import edu.mit.mobile.android.content.GenericDBHelper;
import edu.mit.mobile.android.content.SimpleContentProvider;


public class MessageProvider extends SimpleContentProvider {

	public static final String
		AUTHORITY = "info.staticfree.android.dearfutureself";

	public MessageProvider() {
		super(AUTHORITY, "content", 11);

		final GenericDBHelper messageHelper = new GenericDBHelper(Message.class, Message.CONTENT_URI);

		addDBHelper(messageHelper);
		addDirUri(messageHelper, Message.PATH, DBHelperMapper.TYPE_ALL);
		addItemUri(messageHelper, Message.PATH + "/#", DBHelperMapper.TYPE_ALL);
	}
}
