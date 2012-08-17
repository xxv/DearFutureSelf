package info.staticfree.android.dearfutureself;

import info.staticfree.android.dearfutureself.content.Message;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {

			Log.d("DearFutureSelf", "got boot message");
			context.startService(new Intent(MessageService.ACTION_SCHEDULE_MESSAGE, Message
					.getUriForStates(Message.STATE_IN_TRANSIT)));
		}
	}
}
