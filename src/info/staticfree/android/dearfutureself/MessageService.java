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
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.BigTextStyle;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.app.NotificationCompat.InboxStyle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;

public class MessageService extends Service {
    private static final String TAG = MessageService.class.getSimpleName();

    private static final boolean DEBUG = BuildConfig.DEBUG;

    /**
     * Show a notification for the {@link Message} provided in the data field.
     */
    public static final String ACTION_SHOW_NOTIFICATION =
            "info.staticfree.android.dearfutureself.ACTION_SHOW_NOTIFICATION";

    /**
     * Schedule one or more {@link Message}s to be delivered in the future. If the destination time
     * has passed, they will arrive immediately.
     */
    public static final String ACTION_SCHEDULE_MESSAGE =
            "info.staticfree.android.dearfutureself.ACTION_SCHEDULE_MESSAGE";

    private static final String[] PROJECTION = { Message._ID, Message.SUBJECT, Message.BODY,
            Message.DATE_ARRIVE, Message.DATE_SENT };
    private static final String[] COUNT_PROJECTION = { Message._ID, Message.STATE };

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

        if (ACTION_SHOW_NOTIFICATION.equals(action)) {
            setState(message, Message.STATE_NEW);
            showNotification(message);
            stopSelf();
        } else if (ACTION_SCHEDULE_MESSAGE.equals(action)) {
            scheduleMessages(message);
            stopSelf();
        }

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    /**
     * @return an Intent which will {@link #ACTION_SCHEDULE_MESSAGE} all
     *         {@link Message#STATE_IN_TRANSIT}.
     */
    public static Intent getScheduleIntent() {
        return new Intent(MessageService.ACTION_SCHEDULE_MESSAGE,
                Message.getUriForStates(Message.STATE_IN_TRANSIT));
    }

    public void scheduleMessages(Uri messages) {

        final Cursor c = getContentResolver().query(messages, null, null, null, null);
        try {
            if (DEBUG) {
                Log.d(TAG, "scheduling " + c.getCount() + " messages(s)");
            }
            final int dateCol = c.getColumnIndex(Message.DATE_ARRIVE);
            final int idCol = c.getColumnIndex(Message._ID);
            for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                final long when = c.getLong(dateCol);
                scheduleMessage(ContentUris.withAppendedId(Message.CONTENT_URI, c.getLong(idCol)),
                        when);
            }
        } finally {
            c.close();
        }
    }

    private void setState(Uri message, int state) {
        final ContentValues cv = new ContentValues();
        cv.put(Message.STATE, state);
        getContentResolver().update(message, cv, null, null);
    }

    public void scheduleMessage(Uri message, long when) {
        final PendingIntent operation =
                PendingIntent.getService(this, 0, new Intent(ACTION_SHOW_NOTIFICATION, message),
                        PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);
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

    public int getCount(ContentResolver cr, int state) {
        final Cursor c =
                cr.query(Message.CONTENT_URI, COUNT_PROJECTION, Message.STATE + "=?",
                        new String[] { String.valueOf(state) }, null);
        c.moveToFirst();
        final int count = c.getCount();
        c.close();
        return count;
    }

    private NotificationCompat.Builder getDefaultNotification(Cursor message) {
        final NotificationCompat.Builder builder =
                new NotificationCompat.Builder(getApplicationContext());
        builder.setSmallIcon(R.drawable.ic_stat_mail_delayed);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));
        builder.setAutoCancel(true);
        builder.setDefaults(Notification.DEFAULT_ALL);

        final long dateArrived =
                message.getLong(message.getColumnIndexOrThrow(Message.DATE_ARRIVE));
        builder.setWhen(dateArrived);

        builder.setTicker(getMessageSubject(message));

        return builder;
    }

    public void showNotification(Uri message) {
        final ContentResolver cr = getContentResolver();

        final Cursor c = cr.query(message, PROJECTION, null, null, null);

        final Cursor newMessages =
                cr.query(Message.CONTENT_URI, PROJECTION, Message.STATE + "=?",
                        new String[] { String.valueOf(Message.STATE_NEW) }, null);
        try {
            if (!c.moveToFirst()) {
                Log.e(TAG, "message " + message + " doesn't seem to actually exist");

                return;
            }

            final NotificationManager nm =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            final NotificationCompat.Builder builder = getDefaultNotification(c);

            final int newMessageCount = newMessages.getCount();

            if (newMessageCount > 1) {
                setMultipleMessageNotification(builder, message, c, newMessages);
            } else {
                setSingleMessageNotification(builder, message, c);
            }

            nm.notify(0, builder.build());
        } finally {
            newMessages.close();
            c.close();
        }
    }

    private Builder setMultipleMessageNotification(Builder builder, Uri message, Cursor c,
            Cursor newMessages) {
        final int newMessageCount = newMessages.getCount();
        final int newMessagesToShow = Math.min(5, newMessageCount);

        final InboxStyle style = new InboxStyle();

        for (int i = 0; i < newMessagesToShow; i++) {
            newMessages.moveToPosition(i);
            final SpannableStringBuilder line = new SpannableStringBuilder();
            final String newMessageSubject =
                    newMessages.getString(newMessages.getColumnIndexOrThrow(Message.SUBJECT))
                            .trim();

            line.append(newMessageSubject);
            line.append(' ');
            line.setSpan(new ForegroundColorSpan(getResources().getColor(android.R.color.white)),
                    0, newMessageSubject.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            line.append(newMessages.getString(newMessages.getColumnIndexOrThrow(Message.BODY))
                    .trim());
            style.addLine(line);
        }

        if (newMessagesToShow < newMessageCount) {
            style.setSummaryText(getString(R.string.notification_messages_plus_n_more,
                    newMessageCount - newMessagesToShow));
        }

        style.setBigContentTitle(getText(R.string.notification_messages_have_arrived));

        final PendingIntent content =
                PendingIntent.getActivity(this, 0, new Intent(Intent.ACTION_VIEW,
                        Message.NEW_MESSAGES).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentIntent(content);
        builder.setNumber(newMessageCount);
        builder.setContentTitle(getText(R.string.notification_messages_have_arrived));

        builder.setStyle(style);

        return builder;

    }

    private Builder setSingleMessageNotification(Builder builder, Uri message, Cursor c) {
        final String body = c.getString(c.getColumnIndex(Message.BODY));

        final BigTextStyle style = new BigTextStyle();

        final PendingIntent content =
                PendingIntent
                        .getActivity(this, 0, new Intent(Intent.ACTION_VIEW, message)
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                PendingIntent.FLAG_UPDATE_CURRENT);
        final CharSequence subject = getMessageSubject(c);

        if (isLongSubject(subject)) {

            final CharSequence fromYourself = getText(R.string.message_from_your_past);
            style.setBigContentTitle(fromYourself);

            final SpannableStringBuilder subjectAndBody = new SpannableStringBuilder(subject);
            subjectAndBody.setSpan(
                    new ForegroundColorSpan(getResources().getColor(android.R.color.white)), 0,
                    subject.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            subjectAndBody.append('\n');
            subjectAndBody.append(body);

            style.bigText(subjectAndBody);

            builder.setContentTitle(fromYourself);
            builder.setSubText(subject);
        } else {

            style.setBigContentTitle(subject);
            style.bigText(body);

            builder.setContentTitle(subject);
            builder.setSubText(body);
        }

        builder.setContentIntent(content);

        final long sentTime = c.getLong(c.getColumnIndexOrThrow(Message.DATE_SENT));

        final CharSequence sentString =
                info.staticfree.android.widget.text.format.DateUtils.getRelativeDateTimeString(
                        this, sentTime, DateUtils.SECOND_IN_MILLIS, DateUtils.YEAR_IN_MILLIS * 10,
                        0);

        style.setSummaryText(sentString);

        builder.setStyle(style);

        return builder;
    }

    private boolean isLongSubject(CharSequence message) {
        return message.toString().trim().length() > 25;
    }

    private CharSequence getMessageSubject(Cursor message) {
        String subject = message.getString(message.getColumnIndex(Message.SUBJECT));

        // default if there's no subject
        if (TextUtils.isEmpty(subject.trim())) {
            subject = getString(R.string.notification_message_arrived);
        }

        return subject;
    }
}
