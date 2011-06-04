package edu.mit.mobile.android.content;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface DBColumn {

	Class<? extends DBColumnType> type();

	boolean notnull() default false;

	boolean primaryKey() default false;

}
