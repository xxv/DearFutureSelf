package edu.mit.mobile.android.content;

import android.database.Cursor;

public class TextColumn extends DBColumnType<String> {

	@Override
	public String toCreateColumn(String colName) {
		return toColumnDef(colName, "TEXT");
	}

	@Override
	public String get(Cursor c, int colNumber) {
		return c.getString(colNumber);
	}
}
