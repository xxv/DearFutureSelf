package info.staticfree.android.dearfutureself;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            if (BuildConfig.DEBUG) {
                Log.d("DearFutureSelf", "got boot message");
            }
            context.startService(MessageService.getScheduleIntent());
        }
    }
}
