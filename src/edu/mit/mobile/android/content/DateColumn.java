package edu.mit.mobile.android.content;

import android.database.Cursor;

public class DateColumn extends DBColumnType<java.util.Date> {

	public final static String CURRENT_TIMESTAMP = "CURRENT_TIMESTAMP";

	@Override
	public String toCreateColumn(String colName) {
		return toColumnDef(colName, "INTEGER");
	}

	@Override
	public java.util.Date get(Cursor c, int colNumber) {
		return new java.util.Date(c.getLong(colNumber));
	}
}
