package info.staticfree.android.dearfutureself.sharedtext;

import android.net.Uri;

/**
 * @author steve
 *
 */
public class SharedTextExtractor implements SharedTextParser {
    private final SharedTextParser[] mParsers = new SharedTextParser[]{new TwitterParser()};

    private SharedTextParser mSuccessfulParser;

    public boolean parse(String sharedText){
        boolean success = false;
        for (final SharedTextParser parser: mParsers){
            success = parser.parse(sharedText);
            if (success){
                mSuccessfulParser = parser;
                break;
            }
        }
        return success;
    }

    public String getBody(){
        return mSuccessfulParser.getBody();
    }

    public String getSubject(){
        return mSuccessfulParser.getSubject();
    }

    @Override
    public Uri getSubjectUri() {
        return mSuccessfulParser.getSubjectUri();
    }

    @Override
    public String getPackageName() {
        return mSuccessfulParser.getPackageName();
    }
}
