package info.staticfree.android.dearfutureself.content;

import android.provider.BaseColumns;

public class Destination implements BaseColumns {
	public static final String
		NAME = "name",
		CREATION_DATE = "creation_date",
		USE_COUNT = "use_count";

	public static final String
		SORT_DEFAULT = USE_COUNT + " DESC, " + CREATION_DATE + " DESC";
}
