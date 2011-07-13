package info.staticfree.android.dearfutureself;

import info.staticfree.android.dearfutureself.content.Message;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.stackoverflow.ArrayUtils;

public class MessageListAdapter extends SimpleCursorAdapter {
	public final static String[] DISPLAYED_FIELDS = new String[]{Message.SUBJECT, Message.BODY, Message.DATE_ARRIVE};
	public final static String[] PROJECTION = ArrayUtils.concat(DISPLAYED_FIELDS,
			new String[]{Message._ID, Message.STATE});

	private int mStateCol, mDateCol;

	public MessageListAdapter(Context context) {
		super(context, R.layout.message_list_item, null,
        		new String[]{Message.SUBJECT, Message.BODY, Message.DATE_ARRIVE},
        		new int[]{android.R.id.text1, android.R.id.text2, R.id.date}, 0);
		setColumns(getCursor());
	}

	@Override
	public void changeCursorAndColumns(Cursor c, String[] from, int[] to) {
		super.changeCursorAndColumns(c, from, to);
		setColumns(c);
	}

	@Override
	public Cursor swapCursor(Cursor c) {
		setColumns(c);
		return super.swapCursor(c);
	}

	@Override
	public void changeCursor(Cursor cursor) {
		setColumns(cursor);
		super.changeCursor(cursor);
	}

	private void setColumns(Cursor c){
		if(c == null){
			return;
		}
		mStateCol = c.getColumnIndex(Message.STATE);
		mDateCol = c.getColumnIndex(Message.DATE_ARRIVE);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final View v = super.getView(position, convertView, parent);

		final Cursor c = (Cursor) getItem(position);

		final int state = c.getInt(mStateCol);

		if (state == Message.STATE_NEW){
			((TextView)v.findViewById(android.R.id.text1)).setTypeface(Typeface.DEFAULT_BOLD);
			((TextView)v.findViewById(android.R.id.text2)).setTypeface(Typeface.DEFAULT_BOLD);
			//v.setBackgroundResource(android.R.drawable.list_selector_background);
		}else{
			((TextView)v.findViewById(android.R.id.text1)).setTypeface(Typeface.DEFAULT);
			((TextView)v.findViewById(android.R.id.text2)).setTypeface(Typeface.DEFAULT);
			//v.setBackgroundColor(mContext.getResources().getColor(R.color.message_background_light_dimmed));
		}

		final long date = c.getLong(mDateCol);

		final CharSequence dateString = DateUtils.getRelativeTimeSpanString(date, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
			//DateUtils.getRelativeDateTimeString(mContext, date, DateUtils.MINUTE_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, DateUtils.FORMAT_ABBREV_TIME);
		((TextView)v.findViewById(R.id.date)).setText(dateString);


		return v;
	}
}
