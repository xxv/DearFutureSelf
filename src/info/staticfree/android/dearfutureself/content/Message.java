package info.staticfree.android.dearfutureself.content;

import android.net.Uri;
import edu.mit.mobile.android.content.DBColumn;
import edu.mit.mobile.android.content.DataItem;
import edu.mit.mobile.android.content.DateColumn;
import edu.mit.mobile.android.content.IntegerColumn;
import edu.mit.mobile.android.content.ProviderUtils;
import edu.mit.mobile.android.content.TextColumn;

public class Message implements DataItem {

	public static final String PATH = "message";

	@DBColumn(type=TextColumn.class, notnull=true)
	public static final String SUBJECT = "subject";

	@DBColumn(type=TextColumn.class)
	public static final String BODY = "body";

	@DBColumn(type=TextColumn.class)
	public static final String TYPE = "type";

	@DBColumn(type=DateColumn.class)
	public static final String DATE_SENT 			= "date_sent";

	@DBColumn(type=IntegerColumn.class)
	public static final String LOCATION_SENT_LAT 	= "location_sent_lat";

	@DBColumn(type=IntegerColumn.class)
	public static final String LOCATION_SENT_LON 	= "location_sent_lon";

	@DBColumn(type=TextColumn.class)
	public static final String DESTINATION 		= "destination";

	public static final Uri
		CONTENT_URI = Uri.parse("content://"+MessageProvider.AUTHORITY+"/"+PATH);

	public static final String
		CONTENT_TYPE_DIR = ProviderUtils.TYPE_DIR_PREFIX + MessageProvider.AUTHORITY + "." + PATH,
		CONTENT_TYPE_ITEM = ProviderUtils.TYPE_DIR_PREFIX + MessageProvider.AUTHORITY + "." + PATH;
}
