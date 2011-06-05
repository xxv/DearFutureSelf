package edu.mit.mobile.android.content;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface DBColumn {

	// this is required because Java doesn't allow null as a default value.
	public static final String NULL = "██████NULL██████";

	@SuppressWarnings("rawtypes")
	Class<? extends DBColumnType> type();

	boolean notnull() default false;

	boolean primaryKey() default false;

	boolean autoIncrement() default false;

	String defaultValue() default NULL;

}
