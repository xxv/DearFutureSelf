package edu.mit.mobile.android.content;

import android.provider.BaseColumns;

public abstract interface DataItem extends BaseColumns {

	@DBColumn(type=IntegerColumn.class, primaryKey=true)
	public static final String _ID = BaseColumns._ID;

}
