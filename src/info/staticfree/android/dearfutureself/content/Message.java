package info.staticfree.android.dearfutureself.content;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import edu.mit.mobile.android.content.ContentItem;
import edu.mit.mobile.android.content.ProviderUtils;
import edu.mit.mobile.android.content.column.DBColumn;
import edu.mit.mobile.android.content.column.DatetimeColumn;
import edu.mit.mobile.android.content.column.IntegerColumn;
import edu.mit.mobile.android.content.column.TextColumn;

public class Message implements ContentItem {

	public static final String PATH = "message";

	public static final int
		STATE_DRAFT      = 0,
		STATE_IN_TRANSIT = 1,
		STATE_NEW        = 2,
		STATE_READ       = 3,
		STATE_DELETED	 = 4;

	@DBColumn(type=TextColumn.class, notnull = true)
	public static final String SUBJECT = "subject";

	@DBColumn(type=TextColumn.class)
	public static final String BODY = "body";

	@DBColumn(type=TextColumn.class)
	public static final String TYPE = "type";

	@DBColumn(type=IntegerColumn.class, defaultValueInt = STATE_DRAFT)
	public static final String STATE = "state";

	@DBColumn(type=DatetimeColumn.class, defaultValue = DatetimeColumn.NOW_IN_MILLISECONDS)
	public static final String DATE_SENT 			= "date_sent";

	@DBColumn(type=DatetimeColumn.class, notnull = true)
	public static final String DATE_ARRIVE = "date_arrive";

	@DBColumn(type=IntegerColumn.class)
	public static final String LOCATION_SENT_LAT 	= "location_sent_lat";

	@DBColumn(type=IntegerColumn.class)
	public static final String LOCATION_SENT_LON 	= "location_sent_lon";

	@DBColumn(type=TextColumn.class)
	public static final String DESTINATION 		= "destination";

	public static final String SORT_DEFAULT = DATE_ARRIVE + " DESC";

	public static boolean setState(ContentResolver cr, Uri message, int state){
		final ContentValues cv = new ContentValues();
		cv.put(STATE, state);
		return cr.update(message, cv, null, null) == 1;
	}

	public static final Uri
		CONTENT_URI = ProviderUtils.toContentUri(MessageProvider.AUTHORITY, PATH);

	public static final String
		CONTENT_TYPE_DIR = ProviderUtils.TYPE_DIR_PREFIX + MessageProvider.AUTHORITY + "." + PATH,
		CONTENT_TYPE_ITEM = ProviderUtils.TYPE_DIR_PREFIX + MessageProvider.AUTHORITY + "." + PATH;
}
