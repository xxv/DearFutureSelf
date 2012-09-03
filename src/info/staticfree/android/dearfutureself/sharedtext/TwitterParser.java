package info.staticfree.android.dearfutureself.sharedtext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.net.Uri;

public class TwitterParser implements SharedTextParser {

    private static final Pattern MATCH_IS_TWITTER = Pattern
            .compile("(.* has shared a tweet with you)");

    private String mSubject, mBody;
    private Uri mUri;

    @Override
    public boolean parse(String subject, String text) {
        final Matcher m = MATCH_IS_TWITTER.matcher(subject);
        if (!m.matches()) {
            return false;
        }

        mSubject = subject;
        mBody = text;

        return true;
    }

    @Override
    public String getBody() {

        return mBody;
    }

    @Override
    public String getSubject() {

        return "Shared from Twitter";
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
