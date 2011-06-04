package edu.mit.mobile.android.content;

import android.database.Cursor;

public abstract class DBColumnType<T> {
	/**
	 *
	 * @param colName
	 * @return
	 */
	public abstract String toCreateColumn(String colName);

	public abstract T get(Cursor c, int colNumber);

	protected String toColumnDef(String colName, String type){
		return "'"+colName+"' "+type;
	}
}
