package info.staticfree.android.dearfutureself.sharedtext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.net.Uri;

/**
 * Parses and recontextualizes shared tweets so the subject is more useful for sharing with
 * yourself. By default, Twitter's subject is geared toward sharing a tweet with someone else and so
 * it is mostly about the person sharing the tweet (in this case, you, so it's always the same).
 *
 */
public class TwitterParser implements SharedTextParser {

    // the below patterns are fragile and are tied to a specific twitter client / version.
    private static final Pattern MATCH_IS_TWITTER = Pattern
            .compile("(.* has shared a tweet with you)");

    private static final Pattern EXTRACT_AUTHOR = Pattern.compile("(.+)\\s--\\s(.+?)");

    private String mSubject;
    private CharSequence mBody;
    private Uri mUri;

    @Override
    public boolean parse(String subject, CharSequence text) {

        // the default subject for Twitter is useful for sending to other people, but not to
        // ourselves.
        Matcher m = MATCH_IS_TWITTER.matcher(subject);
        if (!m.matches()) {
            return false;
        }

        m = EXTRACT_AUTHOR.matcher(text);
        if (m.matches()) {
            mSubject = String.format("tweet from %s", m.group(2));
            mBody = m.group(1);
        } else {
            mSubject = "Shared from Twitter";
            mBody = text;
        }

        return true;
    }

    @Override
    public CharSequence getBody() {

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
