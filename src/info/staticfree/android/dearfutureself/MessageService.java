package info.staticfree.android.dearfutureself;

import info.staticfree.android.dearfutureself.content.Message;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

public class MessageService extends Service {
	private static final String TAG = MessageService.class.getSimpleName();

	public static final String
		ACTION_SHOW_NOTIFICATION = "info.staticfree.android.dearfutureself.ACTION_SHOW_NOTIFICATION",
		ACTION_SCHEDULE_MESSAGE = "info.staticfree.android.dearfutureself.ACTION_SCHEDULE_MESSAGE";

	private static final String[] PROJECTION = {Message._ID, Message.SUBJECT, Message.BODY};

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		final String action = intent.getAction();
		final Uri message = intent.getData();

		if (ACTION_SHOW_NOTIFICATION.equals(action)){
			setState(message, Message.STATE_NEW);
			showNotification(message);
			stopSelf();
		}else if (ACTION_SCHEDULE_MESSAGE.equals(action)){
			scheduleMessages(message);
			stopSelf();
		}

		return START_NOT_STICKY;
	}

	private AlarmManager am;

	@Override
	public void onCreate() {
		super.onCreate();

		am = (AlarmManager) getSystemService(ALARM_SERVICE);
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public void scheduleMessages(Uri messages){

		final Cursor c = getContentResolver().query(messages, null, null, null, null);
		try{
			final int dateCol = c.getColumnIndex(Message.DATE_ARRIVE);
			final int idCol = c.getColumnIndex(Message._ID);
			for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()){
				final long when = c.getLong(dateCol);
				scheduleMessage(ContentUris.withAppendedId(Message.CONTENT_URI, c.getLong(idCol)), when);
			}
		}finally{
			c.close();
		}
	}

	private void setState(Uri message, int state){
		final ContentValues cv = new ContentValues();
		cv.put(Message.STATE, state);
		getContentResolver().update(message, cv, null, null);
	}

	public void scheduleMessage(Uri message, long when){
		final PendingIntent operation = PendingIntent.getService(this, 0,
				new Intent(ACTION_SHOW_NOTIFICATION, message),
						PendingIntent.FLAG_ONE_SHOT);
		am.cancel(operation);
		am.set(AlarmManager.RTC_WAKEUP, when, operation);
	}

	public void showNotification(Uri message){
		final Cursor c = getContentResolver().query(message, PROJECTION, null, null, null);
		if (!c.moveToFirst()){
			Log.e(TAG, "message " + message + " doesn't seem to actually exist");
			return;
		}
		final String subject = c.getString(c.getColumnIndex(Message.SUBJECT));
		final String body = c.getString(c.getColumnIndex(Message.BODY));

		final long id = ContentUris.parseId(message);
		final NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		final Notification notification = new Notification(R.drawable.ic_stat_mail_delayed, subject, System.currentTimeMillis());
		notification.flags = Notification.FLAG_AUTO_CANCEL;
		notification.defaults = Notification.DEFAULT_ALL;
		final PendingIntent content = PendingIntent.getActivity(this, 0, new Intent(Intent.ACTION_VIEW, message).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_UPDATE_CURRENT);
		notification.setLatestEventInfo(this, subject, body, content);
		notification.tickerText="Your message has arrived.";

		nm.notify((int) id, notification);
	}

}
