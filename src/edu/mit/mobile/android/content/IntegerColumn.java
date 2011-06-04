package edu.mit.mobile.android.content;

import android.database.Cursor;

public class IntegerColumn extends DBColumnType<java.lang.Integer> {

	@Override
	public String toCreateColumn(String colName) {
		return toColumnDef(colName, "INTEGER");
	}

	@Override
	public java.lang.Integer get(Cursor c, int colNumber) {

		return java.lang.Integer.valueOf(c.getInt(colNumber));
	}

}
