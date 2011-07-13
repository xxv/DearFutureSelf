package info.staticfree.android.dearfutureself.sharedtext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.net.Uri;

public class TwitterParser implements SharedTextParser {

	private static final Pattern PARSE_RE = Pattern.compile("(.* has shared a Tweet with you):\\s*\"(.*)\"\\s*--(.+)");

	private String mSubject, mBody;
	private Uri mUri;

	@Override
	public boolean parse(String text) {
		final Matcher m = PARSE_RE.matcher(text);
		if (! m.matches()){
			return false;
		}

		mSubject = m.group(1);
		mBody = m.group(2);
		mUri = Uri.parse(m.group(3));

		return true;
	}

	@Override
	public String getBody() {

		return mBody;
	}

	@Override
	public String getSubject() {

		return mSubject;
	}

	@Override
	public String getPackageName() {
		return "com.twitter.android";
	}

	@Override
	public Uri getSubjectUri() {

		return mUri;
	}

}
