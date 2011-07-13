package info.staticfree.android.dearfutureself.content;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

public class MessageUtils {
	public static class SetStateTask extends AsyncTask<Object, Void, Boolean> {
		private final Context mContext;
		public SetStateTask(Context context) {
			mContext = context;
		}

		@Override
		protected Boolean doInBackground(Object... params) {
			return Message.setState(mContext.getContentResolver(), (Uri)params[0], ((Integer)params[1]).intValue());
		}
	}
}


