package info.staticfree.android.dearfutureself;

import info.staticfree.android.dearfutureself.content.Message;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.text.format.DateUtils;
import android.util.Log;

public class MessageService extends Service {
    private static final String TAG = MessageService.class.getSimpleName();

    private static final boolean DEBUG = false;

    public static final String
        ACTION_SHOW_NOTIFICATION = "info.staticfree.android.dearfutureself.ACTION_SHOW_NOTIFICATION",
        ACTION_SCHEDULE_MESSAGE = "info.staticfree.android.dearfutureself.ACTION_SCHEDULE_MESSAGE";

    private static final String[] PROJECTION = {Message._ID, Message.SUBJECT, Message.BODY, Message.DATE_ARRIVE};
    private static final String[] COUNT_PROJECTION = {Message._ID, Message.STATE};

    private AlarmManager am;

    @Override
    public void onCreate() {
        super.onCreate();

        am = (AlarmManager) getSystemService(ALARM_SERVICE);
    }

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

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    public void scheduleMessages(Uri messages){

        final Cursor c = getContentResolver().query(messages, null, null, null, null);
        try{
            if (DEBUG) {
                Log.d(TAG, "scheduling " + c.getCount() + " messages(s)");
            }
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
        if (DEBUG) {
            Log.d(TAG,
                    "scheduled display of "
                            + message
                            + " at "
                            + DateUtils.formatDateTime(this, when, DateUtils.FORMAT_SHOW_DATE
                                    | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_YEAR));
        }
    }

    public int getCount(ContentResolver cr, int state){
        final Cursor c = cr.query(Message.CONTENT_URI, COUNT_PROJECTION, Message.STATE+"=?", new String[]{String.valueOf(state)}, null);
        c.moveToFirst();
        final int count = c.getCount();
        c.close();
        return count;
    }

    @SuppressWarnings("deprecation")
    // the replacement for Notification is introduced in API level 11
    public void showNotification(Uri message){
        final ContentResolver cr = getContentResolver();

        final Cursor c = cr.query(message, PROJECTION, null, null, null);
        try{
            if (!c.moveToFirst()){
                Log.e(TAG, "message " + message + " doesn't seem to actually exist");

                return;
            }

            String subject = c.getString(c.getColumnIndex(Message.SUBJECT));
            final String body = c.getString(c.getColumnIndex(Message.BODY));
            final long dateArrived = c.getLong(c.getColumnIndex(Message.DATE_ARRIVE));

            // default if there's no subject
            if (subject == null || "".equals(subject.trim())){
                subject = getString(R.string.notification_message_arrived);
            }

            final NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            // the ticker text for the notification is just the subject.
            final Notification notification = new Notification(R.drawable.ic_stat_mail_delayed, subject, dateArrived);
            notification.flags = Notification.FLAG_AUTO_CANCEL;
            notification.defaults = Notification.DEFAULT_ALL;

            final int newCount = getCount(cr, Message.STATE_NEW);
            if (newCount > 1){
                final PendingIntent content = PendingIntent
                        .getActivity(this, 0, new Intent(Intent.ACTION_VIEW, Message.NEW_MESSAGES)
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                PendingIntent.FLAG_UPDATE_CURRENT);
                notification.setLatestEventInfo(this, getString(R.string.notification_messages_have_arrived), getString(R.string.you_have_x_new_messages, newCount), content);
                notification.number = newCount;

            }else{
                final PendingIntent content = PendingIntent.getActivity(this, 0, new Intent(Intent.ACTION_VIEW, message).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_UPDATE_CURRENT);
                notification.setLatestEventInfo(this, subject, body, content);
            }

            nm.notify(0, notification);
        }finally{
            c.close();
        }
    }
}
