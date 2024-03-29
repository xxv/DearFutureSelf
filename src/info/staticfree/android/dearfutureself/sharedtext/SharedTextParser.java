package info.staticfree.android.dearfutureself.sharedtext;

import android.net.Uri;

public interface SharedTextParser {

    /**
     * @param sharedText
     *            the shared text to parse
     * @return true if the given text is from the specified source.
     */
    public boolean parse(String subject, CharSequence sharedText);
    /**
     * @return the extracted body. Must call {@link #parse(String)} first.
     */
    public CharSequence getBody();
    /**
     * @return the extracted subject. Must call {@link #parse(String)} first.
     */
    public String getSubject();

    public Uri getSubjectUri();

    /**
     * This is used to get the icon to represent this type of share.
     *
     * @return the name of the package of the application that generated the shared text.
     */
    public String getPackageName();
}
